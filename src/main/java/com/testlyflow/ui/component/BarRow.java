package com.testlyflow.ui.component;

import com.vaadin.flow.component.html.Div;

public class BarRow extends Div {

    public BarRow(String label, double fillPercent, String valueText) {
        this(label, fillPercent, valueText, false);
    }

    public BarRow(String label, double fillPercent, String valueText, boolean accent) {
        addClassName("bar-row");
        Div lab = new Div();
        lab.addClassName("bar-label");
        lab.setText(label);
        Div track = new Div();
        track.addClassName("bar-track");
        Div fill = new Div();
        fill.addClassName("bar-fill");
        if (accent) {
            fill.addClassName("bar-fill-accent");
        }
        double pct = Math.max(0, Math.min(100, fillPercent));
        fill.getStyle().set("width", pct + "%");
        track.add(fill);
        Div value = new Div();
        value.addClassName("bar-value");
        value.setText(valueText);
        add(lab, track, value);
    }
}
