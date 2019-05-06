package net.ravendb.demo;

import java.io.IOException;

import org.claspina.confirmdialog.ConfirmDialog;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import net.ravendb.demo.views.MainHeader;
import net.ravendb.demo.views.MainMenu;

@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Route(value = "")
@Theme(Lumo.class)
@HtmlImport("frontend://styles/shared-styles.html")
@PageTitle(value = "Hospital Management")
public class RavenDBApp extends Composite<VerticalLayout> implements RouterLayout, SessionInitListener {

    private VerticalLayout contentLayout = new VerticalLayout();
    private MainHeader mainHeader = new MainHeader();
    private MainMenu mainMenu = new MainMenu();

    public RavenDBApp() {
        this.init();
    }

    private void init() {
        ConfirmDialog.setButtonDefaultIconsVisible(false);
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);

        HorizontalLayout horizontalLayout = new HorizontalLayout(mainMenu, contentLayout);
        horizontalLayout.setSizeFull();
        horizontalLayout.setMargin(false);
        horizontalLayout.setPadding(false);
        horizontalLayout.setSpacing(false);

        horizontalLayout.setFlexGrow(1, mainMenu);
        horizontalLayout.setFlexGrow(4, contentLayout);

        getContent().add(mainHeader, horizontalLayout);
        getContent().setAlignSelf(Alignment.CENTER, horizontalLayout);
        getContent().setSizeFull();
        getContent().setPadding(false);
        getContent().setSpacing(false);


    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentLayout.removeAll();
        contentLayout.getElement().appendChild(content.getElement());
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        System.out.println("-----------------------");
        event.getSession().addRequestHandler(new ImageRequestHandler());
    }

    private static class ImageRequestHandler implements RequestHandler {

        @Override
        public boolean handleRequest(VaadinSession session, VaadinRequest request,
                                     VaadinResponse response) throws IOException {
            System.out.println(request.getPathInfo());
            return false;
        }
    }

}
