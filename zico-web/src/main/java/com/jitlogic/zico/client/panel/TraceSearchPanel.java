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
package com.jitlogic.zico.client.panel;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ClientUtil;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.data.SymbolProxy;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchQueryProxy;
import com.jitlogic.zico.shared.data.TraceInfoSearchResultProxy;
import com.jitlogic.zico.shared.services.TraceDataServiceProxy;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.button.ToggleButton;
import com.sencha.gxt.widget.core.client.container.BoxLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.form.validator.RegExValidator;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.tips.ToolTipConfig;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraceSearchPanel extends VerticalLayoutContainer {

    public final static String RE_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}\\s*(\\d{2}:\\d{2}:\\d{2}(\\.\\d{1-3})?)?";

    private PanelFactory pf;
    private ZicoRequestFactory rf;

    private ErrorHandler errorHandler;

    private Provider<ZicoShell> shell;

    private HostProxy host;

    private DataGrid<TraceInfoProxy> grid;
    private ListDataProvider<TraceInfoProxy> data;
    private SingleSelectionModel<TraceInfoProxy> selection;
    private TraceSearchTableBuilder rowBuilder;
    private Set<Long> expandedDetails = new HashSet<Long>();

    private Map<Integer, String> traceTypes;

    private int seqnum = 0;

    // Search toolbar controls (in order of occurence on panel toolbar)
    private ToggleButton btnDeepSearch;
    private ToggleButton btnErrors;
    private ToggleButton btnReverse;
    private TextButton btnTraceType;
    private String strTraceType;
    private SpinnerField<Double> txtDuration;
    private ToggleButton btnEnableEql;
    private TextField txtFilter;
    private TextField txtSinceDate;
    private TextButton btnRunSearch;
    private TextButton btnClearFilters;

    private HorizontalPanel statusBar;
    private Label statusLabel;
    private Hyperlink lnkCancelSearch, lnkMore50Results, lnkMore250Results;

    private Menu contextMenu;
    private boolean moreResults;


    @Inject
    public TraceSearchPanel(Provider<ZicoShell> shell, ZicoRequestFactory rf,
                            PanelFactory pf, @Assisted HostProxy host,
                            ErrorHandler errorHandler) {
        this.shell = shell;
        this.rf = rf;
        this.pf = pf;
        this.host = host;
        this.errorHandler = errorHandler;

        traceTypes = new HashMap<Integer, String>();
        traceTypes.put(0, "(all)");

        createToolbar();
        createTraceGrid();
        createStatusBar();
        createContextMenu();

        loadTraceTypes();

        refresh();
    }

    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        btnDeepSearch = new ToggleButton();
        btnDeepSearch.setIcon(Resources.INSTANCE.methodTreeIcon());
        btnDeepSearch.setToolTip("Search whole call trees.");
        toolBar.add(btnDeepSearch);

        btnErrors = new ToggleButton();
        btnErrors.setIcon(Resources.INSTANCE.errorMarkIcon());
        btnErrors.setToolTip("Show only erros.");
        toolBar.add(btnErrors);

        btnReverse = new ToggleButton();
        btnReverse.setIcon(Resources.INSTANCE.sort());
        btnReverse.setToolTip("Reverse order");
        btnReverse.setValue(true);
        toolBar.add(btnReverse);

        btnTraceType = new TextButton();
        btnTraceType.setIcon(Resources.INSTANCE.filterIcon());
        btnTraceType.setToolTip("Filter by trace type");
        btnTraceType.setMenu(new Menu());
        toolBar.add(btnTraceType);

        SeparatorToolItem separator = new SeparatorToolItem();
        separator.setWidth(10);
        toolBar.add(separator);

        txtDuration = new SpinnerField<Double>(new NumberPropertyEditor.DoublePropertyEditor());
        txtDuration.setIncrement(1d);
        txtDuration.setMinValue(0);
        txtDuration.setMaxValue(1000000d);
        txtDuration.setAllowNegative(false);
        txtDuration.setAllowBlank(true);
        txtDuration.setWidth(80);
        txtDuration.setToolTip("Minimum trace execution time (in seconds)");
        toolBar.add(txtDuration);


        txtDuration.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    refresh();
                }
            }
        });


        btnEnableEql = new ToggleButton();
        btnEnableEql.setIcon(Resources.INSTANCE.eqlIcon());
        btnEnableEql.setToolTip("EQL query (instead of full-text query)");

        toolBar.add(btnEnableEql);


        txtFilter = new TextField();
        BoxLayoutContainer.BoxLayoutData txtFilterLayout = new BoxLayoutContainer.BoxLayoutData();
        txtFilterLayout.setFlex(1.0);

        ToolTipConfig ttcFilter = new ToolTipConfig("Text search:" +
                "<li><b>sometext</b> - full-text search</li>"
                + "<li><b>~regex</b> - regular expression search</li>"
                + "<li>Regular expression queries if <b>QL</b> is enabled.</li>");

        txtFilter.setToolTipConfig(ttcFilter);

        txtFilter.setLayoutData(txtFilterLayout);
        toolBar.add(txtFilter);


        txtFilter.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    refresh();
                }
            }
        });

        txtSinceDate = new TextField();
        txtSinceDate.setWidth(130);
        txtSinceDate.setEmptyText("Older than ...");
        txtSinceDate.addValidator(new RegExValidator(RE_TIMESTAMP, "Enter valid timestamp"));

        ToolTipConfig ttcDateTime = new ToolTipConfig("Allowed timestamp formats:" +
                "<li><b>YYYY-MM-DD</b> - date only</li>" +
                "<li><b>YYYY-MM-DD hh:mm:ss</b> - date and time</li>" +
                "<li><b>YYYY-MM-DD hh:mm:ss.SSS</b> - millisecond resolution</li>");

        txtSinceDate.setToolTipConfig(ttcDateTime);

        txtSinceDate.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    refresh();
                }
            }
        });

        toolBar.add(txtSinceDate);

        btnRunSearch = new TextButton();
        btnRunSearch.setIcon(Resources.INSTANCE.searchIcon());
        btnRunSearch.setToolTip("Search.");
        toolBar.add(btnRunSearch);

        btnRunSearch.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                refresh();
            }
        });

        toolBar.add(new SeparatorToolItem());

        btnClearFilters = new TextButton();
        btnClearFilters.setIcon(Resources.INSTANCE.clearIcon());
        btnClearFilters.setToolTip("Clear all filters and refresh");
        toolBar.add(btnClearFilters);


        btnClearFilters.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                txtFilter.setText("");
                txtDuration.setText("");
                btnErrors.setValue(false);
                strTraceType = null;

                refresh();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }

    private void createTraceGrid() {
        grid = new DataGrid<TraceInfoProxy>(1024*1024, KEY_PROVIDER);
        selection = new SingleSelectionModel<TraceInfoProxy>(KEY_PROVIDER);
        grid.setSelectionModel(selection);

        // TODO detail expander cell here

        Column<TraceInfoProxy, TraceInfoProxy> colExpander
                = new IdentityColumn<TraceInfoProxy>(DETAIL_EXPANDER_CELL);
        grid.addColumn(colExpander, "#");
        grid.setColumnWidth(colExpander, 32, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceClock
                = new IdentityColumn<TraceInfoProxy>(TRACE_CLOCK_CELL);
        grid.addColumn(colTraceClock, new ResizableHeader<TraceInfoProxy>("Clock", grid, colTraceClock));
        grid.setColumnWidth(colTraceClock, 140, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceType
                = new IdentityColumn<TraceInfoProxy>(TRACE_TYPE_CELL);
        grid.addColumn(colTraceType, new ResizableHeader<TraceInfoProxy>("Type", grid, colTraceType));
        grid.setColumnWidth(colTraceType, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceDuration
                = new IdentityColumn<TraceInfoProxy>(TRACE_DURATION_CELL);
        grid.addColumn(colTraceDuration, new ResizableHeader<TraceInfoProxy>("Time", grid, colTraceDuration));
        grid.setColumnWidth(colTraceDuration, 64, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceCalls
                = new IdentityColumn<TraceInfoProxy>(TRACE_CALLS_CELL);
        grid.addColumn(colTraceCalls, new ResizableHeader<TraceInfoProxy>("Calls", grid, colTraceCalls));
        grid.setColumnWidth(colTraceCalls, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceErrors
                = new IdentityColumn<TraceInfoProxy>(TRACE_ERRORS_CELL);
        grid.addColumn(colTraceErrors, new ResizableHeader<TraceInfoProxy>("Errs", grid, colTraceErrors));
        grid.setColumnWidth(colTraceErrors, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceRecords
                = new IdentityColumn<TraceInfoProxy>(TRACE_RECORDS_CELL);
        grid.addColumn(colTraceRecords, new ResizableHeader<TraceInfoProxy>("Recs", grid, colTraceRecords));
        grid.setColumnWidth(colTraceRecords, 50, Style.Unit.PX);

        Column<TraceInfoProxy, TraceInfoProxy> colTraceDesc
                = new IdentityColumn<TraceInfoProxy>(TRACE_NAME_CELL);
        grid.addColumn(colTraceDesc, "Description");
        grid.setColumnWidth(colTraceDesc, 100, Style.Unit.PCT);

        rowBuilder = new TraceSearchTableBuilder(grid, expandedDetails);
        grid.setTableBuilder(rowBuilder);

        grid.setSkipRowHoverStyleUpdate(true);
        grid.setSkipRowHoverFloatElementCheck(true);
        grid.setSkipRowHoverCheck(true);
        grid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        data = new ListDataProvider<TraceInfoProxy>();
        data.addDataDisplay(grid);

        grid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceInfoProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceInfoProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selection.setSelected(event.getValue(), true);
                    openDetailView();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selection.setSelected(event.getValue(), true);
                    if (selection.getSelectedObject() != null) {
                        contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                    }
                }
            }
        });

        grid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        grid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        add(grid, new VerticalLayoutData(1, 1));
    }

    private void createStatusBar() {
        statusBar = new HorizontalPanel();
        statusLabel = new Label("Ready.");

        lnkCancelSearch = new Hyperlink("Cancel search", "");
        lnkCancelSearch.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toggleSearchMode(false, moreResults, "Search canceled.");
            }
        }, ClickEvent.getType());

        lnkMore50Results = new Hyperlink("[50 more results]", "");
        lnkMore50Results.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadMore(50);
            }
        }, ClickEvent.getType());

        lnkMore250Results = new Hyperlink("[250 more results]", "");
        lnkMore250Results.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadMore(250);
            }
        }, ClickEvent.getType());

        statusBar.add(statusLabel);

        HorizontalPanel hp = new HorizontalPanel();
        hp.getElement().addClassName(Resources.INSTANCE.zicoCssResources().searchStatusBar());
        statusBar.getElement().addClassName(Resources.INSTANCE.zicoCssResources().searchStatusBarInt());
        hp.setWidth("100%");
        HorizontalPanel spacer = new HorizontalPanel(); spacer.setWidth("100px"); hp.add(spacer);
        hp.add(statusBar);

        add(hp);
    }

    private void createContextMenu() {
        contextMenu = new Menu();

        MenuItem mnuMethodTree = new MenuItem("Method call tree");
        mnuMethodTree.setIcon(Resources.INSTANCE.methodTreeIcon());
        mnuMethodTree.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openDetailView();
            }
        });
        contextMenu.add(mnuMethodTree);

        MenuItem mnuMethodRank = new MenuItem("Method call stats");
        mnuMethodRank.setIcon(Resources.INSTANCE.methodRankIcon());
        mnuMethodRank.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openRankingView();
            }
        });
        contextMenu.add(mnuMethodRank);

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuMethodAttrs = new MenuItem("Trace Attributes");
        mnuMethodAttrs.setIcon(Resources.INSTANCE.methodAttrsIcon());
        mnuMethodAttrs.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                openMethodAttrsDialog();
            }
        });
        contextMenu.add(mnuMethodAttrs);
    }


    private void toggleSearchMode(boolean inSearch, boolean moreResults, String message) {
        seqnum++;
        btnDeepSearch.setEnabled(!inSearch);
        btnErrors.setEnabled(!inSearch);
        btnTraceType.setEnabled(!inSearch);
        txtDuration.setEnabled(!inSearch);
        btnEnableEql.setEnabled(!inSearch);
        txtFilter.setEnabled(!inSearch);
        btnRunSearch.setEnabled(!inSearch);
        btnClearFilters.setEnabled(!inSearch);

        statusLabel.setText(message);

        if (inSearch) {
            statusBar.add(lnkCancelSearch);
        } else {
            statusBar.remove(lnkCancelSearch);
        }

        if (moreResults) {
            statusBar.add(lnkMore50Results);
            statusBar.add(lnkMore250Results);
        } else {
            statusBar.remove(lnkMore250Results);
            statusBar.remove(lnkMore50Results);
        }

    }


    private void openDetailView() {
        TraceInfoProxy traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            TraceCallTreePanel detail = pf.traceCallTreePanel(traceInfo);
            shell.get().addView(detail, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }


    private void openRankingView() {
        TraceInfoProxy traceInfo = selection.getSelectedObject();
        if (traceInfo != null) {
            MethodRankingPanel ranking = pf.methodRankingPanel(traceInfo);
            shell.get().addView(ranking, ClientUtil.formatTimestamp(traceInfo.getClock()) + "@" + host.getName());
        }
    }

    private void openMethodAttrsDialog() {
        TraceInfoProxy ti = selection.getSelectedObject();
        if (ti != null) {
            pf.methodAttrsDialog(ti.getHostName(), ti.getDataOffs(), "", 0L).show();
        }
    }

    private void refresh() {
        data.getList().clear();
        expandedDetails.clear();
        loadMore();
    }

    private void loadMore() {
        loadMore(50);
    }

    private void loadMore(final int limit) {
        toggleSearchMode(true, false, "Searching ...");
        TraceDataServiceProxy req = rf.traceDataService();
        TraceInfoSearchQueryProxy q = req.create(TraceInfoSearchQueryProxy.class);
        q.setLimit(limit);
        q.setHostName(host.getName());
        q.setSeq(seqnum);

        q.setFlags(
                (btnErrors.getValue() ? TraceInfoSearchQueryProxy.ERRORS_ONLY : 0)
              | (btnReverse.getValue() ? TraceInfoSearchQueryProxy.ORDER_DESC : 0)
              | (btnDeepSearch.getValue() ? TraceInfoSearchQueryProxy.DEEP_SEARCH : 0)
              | (btnEnableEql.getValue() ? TraceInfoSearchQueryProxy.EQL_QUERY : 0)
        );

        List<TraceInfoProxy> list = data.getList();
        if (list.size() > 0) {
            q.setOffset(list.get(list.size()-1).getDataOffs());
        }

        if (strTraceType != null) {
            q.setTraceName(strTraceType);
        }

        if (txtFilter.getText() != null && txtFilter.getText().length() > 0) {
            q.setSearchExpr(txtFilter.getText());
        }


        if (txtSinceDate.getCurrentValue() != null) {
            q.setSinceDate(ClientUtil.parseTimestamp(txtSinceDate.getCurrentValue(), null));
        }

        if (txtDuration.getCurrentValue() != null) {
            q.setMinMethodTime((long) (txtDuration.getCurrentValue() * 1000000000L));
        }

        req.searchTraces(q).fire(new Receiver<TraceInfoSearchResultProxy>() {
            @Override
            public void onSuccess(TraceInfoSearchResultProxy response) {
                if (response.getSeq() == seqnum) {
                    List<TraceInfoProxy> results = response.getResults();
                    data.getList().addAll(results);
                    moreResults = 0 != (response.getFlags() & TraceInfoSearchResultProxy.MORE_RESULTS);
                    toggleSearchMode(false, moreResults, "Found " + data.getList().size() + " results.");
                    if (moreResults && results.size() < limit) {
                        loadMore(limit-results.size());
                    }
                }
            }

            @Override
            public void onFailure(ServerFailure error) {
                toggleSearchMode(false, false, "Error occured while searching: " + error.getMessage());
                errorHandler.error("Trace search request failed", error);
            }
        });
    }

    private void loadTraceTypes() {
        rf.systemService().getTidMap(host.getName()).fire(new Receiver<List<SymbolProxy>>() {
            @Override
            public void onSuccess(List<SymbolProxy> response) {
                Menu menu = new Menu();

                MenuItem miAll = new MenuItem("<all>");
                miAll.addSelectionHandler(new SelectionHandler<Item>() {
                    @Override
                    public void onSelection(SelectionEvent<Item> event) {
                        strTraceType = null;
                        refresh();
                    }
                });
                menu.add(miAll);
                menu.add(new SeparatorMenuItem());

                for (final SymbolProxy e : response) {
                    MenuItem item = new MenuItem(e.getName());
                    item.addSelectionHandler(new SelectionHandler<Item>() {
                        @Override
                        public void onSelection(SelectionEvent<Item> event) {
                            strTraceType = e.getName();
                            refresh();
                        }
                    });
                    menu.add(item);
                }

                btnTraceType.setMenu(menu);
            }

            @Override
            public void onFailure(ServerFailure error) {
                errorHandler.error("Error loading TID map", error);
            }
        });
    }

    private void toggleDetails(TraceInfoProxy ti) {
        long offs = ti.getDataOffs();
        if (expandedDetails.contains(offs)) {
            expandedDetails.remove(offs);
        } else {
            expandedDetails.add(offs);
        }
        grid.redrawRow(data.getList().indexOf(ti));
    }

    private static final ProvidesKey<TraceInfoProxy> KEY_PROVIDER = new ProvidesKey<TraceInfoProxy>() {
        @Override
        public Object getKey(TraceInfoProxy item) {
            return item.getDataOffs();
        }
    };

    private final static String SMALL_CELL_CSS = Resources.INSTANCE.zicoCssResources().traceSmallCell();

    private static final String EXPANDER_EXPAND = AbstractImagePrototype.create(Resources.INSTANCE.expanderExpand()).getHTML();
    private static final String EXPANDER_COLLAPSE = AbstractImagePrototype.create(Resources.INSTANCE.expanderCollapse()).getHTML();

    private final Cell<TraceInfoProxy> DETAIL_EXPANDER_CELL = new ActionCell<TraceInfoProxy>("",
            new ActionCell.Delegate<TraceInfoProxy>() {
                @Override
                public void execute(TraceInfoProxy rec) {
                    toggleDetails(rec);
                }
            }) {
        @Override
        public void render(Cell.Context context, TraceInfoProxy tr, SafeHtmlBuilder sb) {
            if ((tr.getAttributes() != null && tr.getAttributes().size() > 0)||tr.getExceptionInfo() != null) {
                sb.appendHtmlConstant("<span style=\"cursor: pointer;\">");
                sb.appendHtmlConstant(expandedDetails.contains(tr.getDataOffs()) ? EXPANDER_COLLAPSE : EXPANDER_EXPAND);
                sb.appendHtmlConstant("</span>");
            }
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_NAME_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy ti, SafeHtmlBuilder sb) {
            String color = ti.getStatus() != 0 ? "red" : "black";
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\" style=\"color: " + color + "; text-align: left;\">");
            sb.append(SafeHtmlUtils.fromString(ti.getDescription()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_CLOCK_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatTimestamp(rec.getClock())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_TYPE_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getTraceType()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_DURATION_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString(ClientUtil.formatDuration(rec.getExecutionTime())));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_CALLS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getCalls()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_RECORDS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getRecords()));
            sb.appendHtmlConstant("</div>");
        }
    };

    private AbstractCell<TraceInfoProxy> TRACE_ERRORS_CELL = new AbstractCell<TraceInfoProxy>() {
        @Override
        public void render(Context context, TraceInfoProxy rec, SafeHtmlBuilder sb) {
            sb.appendHtmlConstant("<div class=\"" + SMALL_CELL_CSS + "\">");
            sb.append(SafeHtmlUtils.fromString("" + rec.getErrors()));
            sb.appendHtmlConstant("</div>");
        }
    };


}
