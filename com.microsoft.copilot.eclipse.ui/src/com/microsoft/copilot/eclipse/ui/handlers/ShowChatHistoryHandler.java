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
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession;

/**
 * Handler for showing the chat history.
 */
public class ShowChatHistoryHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    ChatServiceManager manager = CopilotUi.getPlugin().getChatServiceManager();
    CopilotSession active = manager != null ? manager.getSessionRegistry().getActive() : null;
    if (eventBroker != null && active != null) {
      eventBroker.post(CopilotEventConstants.TOPIC_CHAT_SHOW_CHAT_HISTORY,
          ChatSessionEvent.forSession(active.getSessionId()));
    }
    return null;
  }
}
