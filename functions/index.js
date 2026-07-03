const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const REGION = "europe-west1";
const TIME_ZONE = "Europe/Belgrade";
const MISSION_KEYS = ["winMatch", "sendChat", "playFriendly", "winTournament"];

function localDay() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: TIME_ZONE, year: "numeric", month: "2-digit", day: "2-digit" })
    .format(new Date());
}

function leagueForStars(stars) {
  const thresholds = [100, 200, 400, 800, 1600];
  return thresholds.filter((threshold) => stars >= threshold).length;
}

function rewardFor(position, monthly) {
  if (position < 1 || position > 10) return 0;
  const weekly = position === 1 ? 5 : position === 2 ? 3 : position === 3 ? 2 : 1;
  return monthly ? weekly * 2 : weekly;
}

async function completeMission(userId, key) {
  if (!userId || !MISSION_KEYS.includes(key)) return;
  const userRef = db.collection("users").doc(userId);
  const missionRef = userRef.collection("dailyMissions").doc(localDay());
  await db.runTransaction(async (transaction) => {
    const mission = await transaction.get(missionRef);
    if (mission.exists && mission.get(key) === true) return;
    const user = await transaction.get(userRef);
    if (!user.exists) return;
    const state = Object.fromEntries(MISSION_KEYS.map((name) => [name, mission.exists && mission.get(name) === true]));
    state[key] = true;
    const allDone = MISSION_KEYS.every((name) => state[name]);
    const alreadyCompleted = mission.exists && mission.get("allRewarded") === true;
    const starReward = 3 + (allDone && !alreadyCompleted ? 3 : 0);
    const stars = Number(user.get("stars") || 0) + starReward;
    transaction.set(missionRef, {
      ...state, allRewarded: alreadyCompleted || allDone,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    transaction.update(userRef, {
      stars, league: leagueForStars(stars),
      tokens: Number(user.get("tokens") || 0) + (allDone && !alreadyCompleted ? 2 : 0),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
}

async function closeCycle(period, cycleId) {
  const marker = db.collection("rankingCycles").doc(`${period}_${cycleId}`);
  const claimed = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(marker);
    if (snapshot.exists) return false;
    transaction.create(marker, {
      period,
      cycleId,
      status: "processing",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return true;
  });
  if (!claimed) return;

  const monthly = period === "monthly";
  const starsField = monthly ? "monthlyStars" : "weeklyStars";
  const matchesField = monthly ? "monthlyMatches" : "weeklyMatches";
  const users = await db.collection("users").get();
  const ranked = users.docs
    .filter((doc) => doc.get("isGuest") !== true)
    .filter((doc) => Number(doc.get(matchesField) || 0) > 0)
    .sort((a, b) => Number(b.get(starsField) || 0) - Number(a.get(starsField) || 0)
      || String(a.get("username") || "").localeCompare(String(b.get("username") || "")));
  const positions = new Map(ranked.map((doc, index) => [doc.id, index + 1]));
  const regionRanks = new Map();
  if (monthly) {
    const totals = new Map();
    for (const user of users.docs) {
      if (user.get("isGuest") === true) continue;
      const region = String(user.get("region") || "");
      if (region) totals.set(region, (totals.get(region) || 0) + Number(user.get(starsField) || 0));
    }
    [...totals.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
      .forEach(([region], index) => regionRanks.set(region, index + 1));
  }

  let batch = db.batch();
  let operations = 0;
  const commitIfFull = async () => {
    if (operations < 400) return;
    await batch.commit(); batch = db.batch(); operations = 0;
  };
  for (const user of users.docs) {
    if (user.get("isGuest") === true) continue;
    const position = positions.get(user.id) || 0;
    const reward = rewardFor(position, monthly);
    let totalStars = Number(user.get("stars") || 0);
    if (monthly && (position === 0 || position > 10)) {
      totalStars -= Math.floor(totalStars * 30 / 100);
    }
    const regionPlace = regionRanks.get(String(user.get("region") || "")) || 0;
    const avatarFrame = monthly
      ? (regionPlace === 1 ? "gold" : regionPlace === 2 ? "silver" : regionPlace === 3 ? "bronze" : "standard")
      : user.get("avatarFrame") || "standard";
    batch.update(user.ref, {
      [starsField]: 0,
      [matchesField]: 0,
      tokens: Number(user.get("tokens") || 0) + reward,
      stars: totalStars,
      league: leagueForStars(totalStars),
      avatarFrame,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    operations++;
    if (position > 0) {
      const notification = user.ref.collection("notifications").doc();
      batch.set(notification, {
        channel: reward > 0 ? "rewards" : "ranking",
        title: monthly ? "Mesecna rang-lista" : "Nedeljna rang-lista",
        message: reward > 0
          ? `Osvojili ste ${position}. mesto i ${reward} tokena.`
          : `Osvojili ste ${position}. mesto.`,
        action: "ranking",
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      operations++;
    }
    await commitIfFull();
  }
  if (monthly) {
    for (const [region, position] of regionRanks.entries()) {
      const values = { region, lastPosition: position, updatedAt: admin.firestore.FieldValue.serverTimestamp() };
      if (position === 1) values.firstPlaces = admin.firestore.FieldValue.increment(1);
      if (position === 2) values.secondPlaces = admin.firestore.FieldValue.increment(1);
      if (position === 3) values.thirdPlaces = admin.firestore.FieldValue.increment(1);
      batch.set(db.collection("regionStats").doc(region), values, { merge: true });
    }
  }
  batch.update(marker, {
    status: "completed",
    participantCount: ranked.length,
    completedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  await batch.commit();
}

exports.closeWeeklyRanking = onSchedule({
  region: REGION, timeZone: TIME_ZONE, schedule: "5 0 * * 1"
}, async () => closeCycle("weekly", new Date().toISOString().slice(0, 10)));

exports.closeMonthlyRanking = onSchedule({
  region: REGION, timeZone: TIME_ZONE, schedule: "10 0 1 * *"
}, async () => closeCycle("monthly", new Date().toISOString().slice(0, 7)));

exports.onChatMessageCreated = onDocumentCreated({
  document: "regionalChats/{region}/messages/{messageId}", region: REGION
}, async (event) => {
  const message = event.data && event.data.data();
  if (!message) return;
  await completeMission(message.senderId, "sendChat");
  const users = await db.collection("users").where("region", "==", event.params.region).get();
  const tokens = [];
  let batch = db.batch();
  for (const user of users.docs) {
    if (user.id === message.senderId) continue;
    const userTokens = Array.isArray(user.get("fcmTokens")) ? user.get("fcmTokens") : [];
    tokens.push(...userTokens);
    batch.set(user.ref.collection("notifications").doc(), {
      channel: "chat", title: `Nova poruka - ${event.params.region}`,
      message: `${message.senderName}: ${message.text}`, action: "chat", read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  }
  await batch.commit();
  for (let index = 0; index < tokens.length; index += 500) {
    await admin.messaging().sendEachForMulticast({
      tokens: tokens.slice(index, index + 500),
      android: { priority: "high" },
      data: { channel: "chat", title: `Nova poruka - ${event.params.region}`,
        message: `${message.senderName}: ${message.text}`, action: "chat" }
    });
  }
});

exports.onMatchSettled = onDocumentUpdated({ document: "matches/{matchId}", region: REGION }, async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (before.settlementApplied === true || after.settlementApplied !== true || after.status !== "finished") return;
  if (after.matchType === "regular" && after.winnerId) await completeMission(after.winnerId, "winMatch");
  if (after.matchType === "friendly") {
    await Promise.all([completeMission(after.player1Id, "playFriendly"), completeMission(after.player2Id, "playFriendly")]);
  }
  if (after.matchType === "tournament" && after.winnerId) await completeMission(after.winnerId, "winTournament");
});

exports._test = { leagueForStars, rewardFor, localDay };
