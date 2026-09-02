// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.UUID;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatSessionEvent;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSessionRegistry;

/**
 * Opens and focuses secondary chat views.
 */
public final class SessionViewOpener {
  private SessionViewOpener() {
  }

  public static ChatView openNewSession() {
    return openNewSession(null);
  }

  public static ChatView openConversation(ConversationXmlData conversation) {
    ChatServiceManager manager = CopilotUi.getPlugin().getChatServiceManager();
    CopilotSessionRegistry registry = manager.getSessionRegistry();
    CopilotSession existing = registry.findByConversation(conversation.getConversationId());
    if (existing != null && existing.getView() != null) {
      activate(existing.getView());
      return existing.getView();
    }
    return openNewSession(conversation);
  }

  public static void focus(ChatView view) {
    activate(view);
  }

  private static ChatView openNewSession(ConversationXmlData conversation) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    IWorkbenchPage page = window != null ? window.getActivePage() : null;
    if (page == null) {
      return null;
    }

    String sessionId = "session-" + UUID.randomUUID();
    try {
      ChatView view = (ChatView) page.showView(Constants.CHAT_VIEW_ID, sessionId, IWorkbenchPage.VIEW_ACTIVATE);
      if (conversation != null) {
        IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
        broker.post(CopilotEventConstants.TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED,
            ChatSessionEvent.forSession(sessionId, conversation));
      }
      view.setFocus();
      return view;
    } catch (PartInitException e) {
      CopilotCore.LOGGER.error("Failed to open Copilot session", e);
      return null;
    }
  }

  private static void activate(ChatView view) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null && window.getActivePage() != null) {
      window.getActivePage().activate(view);
      view.setFocus();
    }
  }
}
