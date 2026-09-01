package com.testlyflow.ui.admin;

import com.testlyflow.ui.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route(value = "admin", layout = MainLayout.class)
public class AdminIndexView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(AdminCategoriesView.class);
    }
}
