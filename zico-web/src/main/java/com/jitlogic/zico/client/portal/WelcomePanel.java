/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.client.portal;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.SymbolProxy;
import com.sencha.gxt.widget.core.client.Portlet;
import com.sencha.gxt.widget.core.client.button.ToolButton;
import com.sencha.gxt.widget.core.client.container.PortalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;

import javax.inject.Provider;
import java.util.List;

public class WelcomePanel implements IsWidget {

    private PortalLayoutContainer portal;

    private ZicoRequestFactory rf;

    private Provider<ZicoShell> shell;

    private SystemInfoPortlet systemInfoPortlet;
    private PanelFactory panelFactory;

    private ErrorHandler errorHandler;

    @Inject
    public WelcomePanel(ZicoRequestFactory rf,
                        SystemInfoPortlet systemInfoPortlet,
                        PanelFactory panelFactory, Provider<ZicoShell> shell,
                        ErrorHandler errorHandler) {

        this.rf = rf;
        this.systemInfoPortlet = systemInfoPortlet;
        this.panelFactory = panelFactory;
        this.shell = shell;
        this.errorHandler = errorHandler;
    }

    @Override
    public Widget asWidget() {
        if (portal == null) {
            createPortal();
        }
        return portal;
    }

    private void createPortal() {

        portal = new PortalLayoutContainer(3);

        portal.getElement().getStyle().setBackgroundColor("white");
        portal.setColumnWidth(0, .50);
        portal.setColumnWidth(1, .25);
        portal.setColumnWidth(2, .25);

        createHelpPortlet();

        //Portlet wndTopHosts = newPortlet("Top Hosts", false);
        //wndTopHosts.add(new HTML("TBD"));
        //portal.add(wndTopHosts, 1);

        //Portlet wndTopOffenders = newPortlet("Top Offenders", false);
        //wndTopOffenders.add(new HTML("TBD"));
        //portal.add(wndTopOffenders, 1);

        portal.add(systemInfoPortlet, 2);

        createUserPortlet();

        rf.userService().isAdminMode().fire(new Receiver<Boolean>() {
            @Override
            public void onSuccess(Boolean isAdmin) {
                if (isAdmin) {
                    createAdminPortlet();
                }
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error performing server request", failure);
            }
        });


    }


    private void createHelpPortlet() {
        Portlet wndHelp = newPortlet("Welcome", true);
        HTML htmlHelp = new HTML(Resources.INSTANCE.tipsHtml().getText());
        VerticalLayoutContainer vp = new VerticalLayoutContainer();
        vp.add(htmlHelp);
        wndHelp.add(vp);
        portal.add(wndHelp, 0);
    }


    private void createAdminPortlet() {
        Portlet wndAdmin = newPortlet("Admin tasks", false);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        Hyperlink lnkTraceDisplayTemplates = new Hyperlink("Trace List Display Templates", "");
        vp.add(lnkTraceDisplayTemplates);

        lnkTraceDisplayTemplates.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openTemplatePanel();
            }
        }, ClickEvent.getType());

        Hyperlink lnkUserManagement = new Hyperlink("Manage users", "");
        vp.add(lnkUserManagement);

        lnkUserManagement.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                shell.get().addView(panelFactory.userManagementPanel(), "User Management");
            }
        }, ClickEvent.getType());

        Hyperlink lnkBackupConfig = new Hyperlink("Back up Symbols and Config", "");
        vp.add(lnkBackupConfig);

        lnkBackupConfig.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                rf.systemService().backupConfig().fire(new Receiver<Void>() {
                    @Override
                    public void onSuccess(Void response) {
                        Window.alert("Symbols and configuration backed up succesfully.");
                    }

                    @Override
                    public void onFailure(ServerFailure failure) {
                        errorHandler.error("Error backing up symbols and configuration.", failure);
                    }
                });
            }
        }, ClickEvent.getType());

        wndAdmin.add(vp);
        portal.add(wndAdmin, 2);
    }


    private void createUserPortlet() {
        Portlet wndUser = newPortlet("My account", false);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        Hyperlink lnkTraceDisplayTemplates = new Hyperlink("Change Password", "");
        vp.add(lnkTraceDisplayTemplates);

        lnkTraceDisplayTemplates.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panelFactory.passwordChangeDialog("").show();
            }
        }, ClickEvent.getType());


        //Hyperlink lnkUsersAccess = new Hyperlink("Users & Access Privileges", "");
        //vp.add(lnkUsersAccess);

        wndUser.add(vp);
        portal.add(wndUser, 2);
    }


    private void openTemplatePanel() {
        shell.get().addView(panelFactory.traceTemplatePanel(), "Templates");
    }


    private Portlet newPortlet(String title, boolean closeable) {
        final Portlet portlet = new Portlet();
        portlet.setHeadingText(title);
        portlet.setCollapsible(true);
        if (closeable) {
            portlet.getHeader().addTool(new ToolButton(ToolButton.CLOSE, new SelectEvent.SelectHandler() {
                @Override
                public void onSelect(SelectEvent event) {
                    portlet.removeFromParent();
                }
            }));
        }
        return portlet;
    }
}
