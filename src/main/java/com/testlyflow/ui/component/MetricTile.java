package com.testlyflow.ui.component;

import com.vaadin.flow.component.html.Div;

public class MetricTile extends Div {

    public MetricTile(String value, String label) {
        addClassName("metric-tile");
        Div valueDiv = new Div();
        valueDiv.addClassName("value");
        valueDiv.setText(value);
        Div labelDiv = new Div();
        labelDiv.addClassName("label");
        labelDiv.setText(label);
        add(valueDiv, labelDiv);
    }
}
