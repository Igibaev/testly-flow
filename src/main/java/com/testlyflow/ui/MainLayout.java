package com.testlyflow.ui;

import com.testlyflow.ui.admin.AdminCategoriesView;
import com.testlyflow.ui.view.HomeView;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends Div implements RouterLayout, AfterNavigationObserver {

    private final Div header = new Div();
    private final RouterLink adminLink = new RouterLink("Админ-панель", AdminCategoriesView.class);
    private final Div content = new Div();

    public MainLayout() {
        addClassName("app-shell");

        header.addClassName("app-header");
        RouterLink title = new RouterLink("Платформа тестирования знаний", HomeView.class);
        title.addClassName("app-title");
        adminLink.addClassName("app-admin-link");
        header.add(title, adminLink);

        content.addClassName("app-content");
        add(header, content);
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        this.content.getElement().removeAllChildren();
        if (content != null) {
            this.content.getElement().appendChild(content.getElement());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        boolean compact = path.matches("attempt/[^/]+") && !path.contains("/result");
        header.getClassNames().set("app-header-compact", compact);
        adminLink.setVisible(!compact);
        getUI().ifPresent(ui -> ui.getPage().setTitle("Платформа тестирования знаний"));
    }
}
