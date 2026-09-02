// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSessionRegistry;
import com.microsoft.copilot.eclipse.ui.handlers.SessionViewOpener;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Master list of open sessions and persisted conversations.
 */
public class CopilotSessionsView extends ViewPart {
  private TableViewer viewer;
  private CopilotSessionRegistry registry;
  private ConversationPersistenceManager persistenceManager;
  private Runnable registryListener;

  @Override
  public void createPartControl(Composite parent) {
    parent.setLayout(new FillLayout());
    ChatServiceManager manager = CopilotUi.getPlugin().getChatServiceManager();
    if (manager == null) {
      return;
    }
    registry = manager.getSessionRegistry();
    persistenceManager = manager.getPersistenceManager();

    viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
    viewer.getTable().setHeaderVisible(true);
    viewer.getTable().setLinesVisible(true);
    viewer.setContentProvider(ArrayContentProvider.getInstance());
    createColumns();
    viewer.addDoubleClickListener(event -> openSelected());

    MenuManager menuManager = new MenuManager();
    menuManager.add(new Action("Open") {
      @Override
      public void run() {
        openSelected();
      }
    });
    menuManager.add(new Action("Rename...") {
      @Override
      public void run() {
        renameSelected();
      }
    });
    menuManager.add(new Action("Delete") {
      @Override
      public void run() {
        deleteSelected();
      }
    });
    Menu menu = menuManager.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuManager, viewer);

    getViewSite().getActionBars().getToolBarManager().add(new Action("New Session") {
      @Override
      public void run() {
        SessionViewOpener.openNewSession();
      }
    });
    getViewSite().getActionBars().getToolBarManager().add(new Action("Refresh") {
      @Override
      public void run() {
        refresh();
      }
    });

    registryListener = () -> {
      if (viewer != null && !viewer.getControl().isDisposed()) {
        viewer.getControl().getDisplay().asyncExec(this::refresh);
      }
    };
    registry.addListener(registryListener);
    refresh();
  }

  private void createColumns() {
    TableViewerColumn title = new TableViewerColumn(viewer, SWT.NONE);
    title.getColumn().setText("Session");
    title.getColumn().setWidth(280);
    title.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((SessionRow) element).title();
      }
    });

    TableViewerColumn status = new TableViewerColumn(viewer, SWT.NONE);
    status.getColumn().setText("Status");
    status.getColumn().setWidth(150);
    status.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((SessionRow) element).status();
      }
    });

    TableViewerColumn updated = new TableViewerColumn(viewer, SWT.NONE);
    updated.getColumn().setText("Updated");
    updated.getColumn().setWidth(150);
    updated.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        Instant instant = ((SessionRow) element).updatedAt();
        return instant != null ? UiUtils.formatRelativeDateTime(instant) : "";
      }
    });
  }

  private void refresh() {
    if (viewer == null || viewer.getControl().isDisposed()) {
      return;
    }
    Map<String, ConversationXmlData> history = new LinkedHashMap<>();
    for (ConversationXmlData conversation : persistenceManager.listConversations()) {
      history.put(conversation.getConversationId(), conversation);
    }

    List<SessionRow> rows = new ArrayList<>();
    for (CopilotSession session : registry.getOpenSessions()) {
      ConversationXmlData conversation = history.remove(session.getConversationId());
      String title = StringUtils.isNotBlank(session.getTitle()) ? session.getTitle()
          : conversation != null ? conversation.getTitle() : "New Copilot session";
      Instant updated = conversation != null && conversation.getLastMessageDate() != null
          ? conversation.getLastMessageDate() : session.getUpdatedAt();
      rows.add(new SessionRow(title, session.getStatus().getLabel(), updated, session, conversation));
    }
    for (ConversationXmlData conversation : history.values()) {
      Instant updated = conversation.getLastMessageDate() != null
          ? conversation.getLastMessageDate() : conversation.getCreationDate();
      rows.add(new SessionRow(conversation.getTitle(), "History", updated, null, conversation));
    }
    rows.sort((left, right) -> {
      Instant leftDate = left.updatedAt() != null ? left.updatedAt() : Instant.EPOCH;
      Instant rightDate = right.updatedAt() != null ? right.updatedAt() : Instant.EPOCH;
      return rightDate.compareTo(leftDate);
    });
    viewer.setInput(rows);
  }

  private SessionRow selectedRow() {
    IStructuredSelection selection = viewer.getStructuredSelection();
    return selection.getFirstElement() instanceof SessionRow row ? row : null;
  }

  private void openSelected() {
    SessionRow row = selectedRow();
    if (row == null) {
      return;
    }
    if (row.session() != null && row.session().getView() != null) {
      SessionViewOpener.focus(row.session().getView());
    } else if (row.conversation() != null) {
      SessionViewOpener.openConversation(row.conversation());
    }
  }

  private void renameSelected() {
    SessionRow row = selectedRow();
    if (row == null || row.conversation() == null) {
      return;
    }
    InputDialog dialog = new InputDialog(getSite().getShell(), "Rename Copilot session",
        "Session name:", row.title(), value -> StringUtils.isBlank(value) ? "Enter a name" : null);
    if (dialog.open() == InputDialog.OK) {
      String newTitle = dialog.getValue().trim();
      persistenceManager.updateConversationTitle(row.conversation().getConversationId(), newTitle)
          .thenRun(() -> {
            if (row.session() != null) {
              registry.updateTitle(row.session().getSessionId(), newTitle);
            }
            registryListener.run();
          });
    }
  }

  private void deleteSelected() {
    SessionRow row = selectedRow();
    if (row == null || row.conversation() == null) {
      return;
    }
    if (row.session() != null && row.session().isOpen()) {
      MessageDialog.openInformation(getSite().getShell(), "Copilot Sessions",
          "Close the chat view before deleting this session.");
      return;
    }
    if (MessageDialog.openConfirm(getSite().getShell(), "Delete Copilot session",
        "Delete \"" + row.title() + "\"?")) {
      persistenceManager.removeConversationById(row.conversation().getConversationId())
          .thenRun(registryListener);
    }
  }

  @Override
  public void setFocus() {
    if (viewer != null) {
      viewer.getControl().setFocus();
    }
  }

  @Override
  public void dispose() {
    if (registry != null && registryListener != null) {
      registry.removeListener(registryListener);
    }
    super.dispose();
  }

  private record SessionRow(String title, String status, Instant updatedAt,
      CopilotSession session, ConversationXmlData conversation) {
  }
}
