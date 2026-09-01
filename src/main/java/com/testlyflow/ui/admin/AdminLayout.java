package com.testlyflow.ui.admin;

import com.testlyflow.ui.MainLayout;
import com.testlyflow.ui.support.AdminSession;
import com.testlyflow.ui.support.NativeUi;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.LinkedHashMap;
import java.util.Map;

@ParentLayout(MainLayout.class)
public class AdminLayout extends Div implements RouterLayout, AfterNavigationObserver {

    private final Div content = new Div();
    private final Map<RouterLink, String> links = new LinkedHashMap<>();

    public AdminLayout() {
        Div nav = new Div();
        nav.addClassName("admin-nav");
        addLink(nav, "Категории", AdminCategoriesView.class, "admin/categories");
        addLink(nav, "Загрузка вопросов", AdminTestsView.class, "admin/tests");
        addLink(nav, "Попытки", AdminAttemptsView.class, "admin/attempts");
        addLink(nav, "Сотрудники", AdminEmployeesView.class, "admin/employees");
        addLink(nav, "Метрики", AdminMetricsView.class, "admin/metrics");
        NativeButton logout = NativeUi.button("Выйти", e -> {
            AdminSession.clear();
            getUI().ifPresent(ui -> ui.navigate(AdminLoginView.class));
        }, "btn", "btn-ghost");
        nav.add(logout);
        add(nav, content);
    }

    private void addLink(Div nav, String caption, Class<? extends com.vaadin.flow.component.Component> view, String path) {
        RouterLink link = new RouterLink(caption, view);
        links.put(link, path);
        nav.add(link);
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
        links.forEach((link, prefix) -> {
            boolean active = path.equals(prefix) || path.startsWith(prefix + "/");
            link.getElement().getClassList().set("active", active);
        });
    }
}
