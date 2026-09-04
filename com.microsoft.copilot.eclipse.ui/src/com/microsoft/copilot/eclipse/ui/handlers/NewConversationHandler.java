// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatSessionEvent;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Handler for creating a new conversation in the chat view.
 */
public class NewConversationHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    if (ConversationUtils.confirmEndChat()) {
      IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
      ChatServiceManager manager = CopilotUi.getPlugin().getChatServiceManager();
      CopilotSession active = manager != null ? manager.getSessionRegistry().getActive() : null;
      if (eventBroker != null && active != null) {
        String sessionId = active.getSessionId();
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_NEW_CONVERSATION,
            ChatSessionEvent.forSession(sessionId));
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY,
            ChatSessionEvent.forSession(sessionId));

        // Reset the title to default
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_CONVERSATION_TITLE_UPDATED,
            ChatSessionEvent.forSession(sessionId, Messages.chat_topBanner_defaultChatTitle));
      }
    }
    return null;
  }
}
