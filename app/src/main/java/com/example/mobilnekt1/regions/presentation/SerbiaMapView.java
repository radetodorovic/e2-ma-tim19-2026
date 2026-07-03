package com.example.mobilnekt1.regions.presentation;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.example.mobilnekt1.regions.domain.RegionPlayerPoint;
import java.util.*;

public final class SerbiaMapView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RegionPlayerPoint> points = new ArrayList<>();

    public SerbiaMapView(Context context, AttributeSet attrs) { super(context, attrs); line.setStrokeWidth(3); }
    public void setPoints(List<RegionPlayerPoint> values) { points.clear(); points.addAll(values); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        Path serbia = new Path();
        serbia.moveTo(w*.27f,h*.05f); serbia.lineTo(w*.68f,h*.08f); serbia.lineTo(w*.78f,h*.28f);
        serbia.lineTo(w*.67f,h*.45f); serbia.lineTo(w*.75f,h*.63f); serbia.lineTo(w*.58f,h*.94f);
        serbia.lineTo(w*.38f,h*.82f); serbia.lineTo(w*.30f,h*.58f); serbia.lineTo(w*.18f,h*.36f); serbia.close();
        fill.setColor(Color.rgb(224,242,241)); canvas.drawPath(serbia, fill);
        line.setStyle(Paint.Style.STROKE); line.setColor(Color.rgb(0,105,92)); canvas.drawPath(serbia, line);
        canvas.save(); canvas.clipPath(serbia);
        line.setColor(Color.LTGRAY); canvas.drawLine(0,h*.30f,w,h*.30f,line); canvas.drawLine(0,h*.52f,w,h*.52f,line);
        canvas.drawLine(w*.48f,h*.30f,w*.48f,h,line);
        for (RegionPlayerPoint point : points) {
            float x = (float) ((point.longitude - 18.7) / (23.1 - 18.7) * w);
            float y = (float) ((46.3 - point.latitude) / (46.3 - 42.2) * h);
            fill.setColor(point.currentUser ? Color.rgb(249,168,37) : Color.rgb(0,137,123));
            canvas.drawCircle(x, y, point.currentUser ? 11 : 7, fill);
        }
        canvas.restore();
    }
}
