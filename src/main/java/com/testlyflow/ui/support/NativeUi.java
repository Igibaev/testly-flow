package com.testlyflow.ui.support;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.NativeButton;

public final class NativeUi {

    private NativeUi() {
    }

    public static NativeButton button(String text, String... classNames) {
        NativeButton button = new NativeButton(text);
        button.addClassNames(classNames);
        return button;
    }

    public static NativeButton button(String text, ComponentEventListener<ClickEvent<NativeButton>> listener,
                                      String... classNames) {
        NativeButton button = button(text, classNames);
        button.addClickListener(listener);
        return button;
    }
}
