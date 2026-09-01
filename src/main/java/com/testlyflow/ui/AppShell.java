package com.testlyflow.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;

@Theme("testly")
@Viewport("width=device-width, initial-scale=1")
@Meta(name = "description", content = "Платформа тестирования знаний сотрудников")
public class AppShell implements AppShellConfigurator {

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.setPageTitle("Платформа тестирования знаний");
        // AppShellSettings.setLanguage arrived after Vaadin 24.3; this is the 24.3 equivalent.
        settings.addInlineWithContents(Inline.Position.PREPEND,
                "document.documentElement.lang='ru';", Inline.Wrapping.JAVASCRIPT);
    }
}
