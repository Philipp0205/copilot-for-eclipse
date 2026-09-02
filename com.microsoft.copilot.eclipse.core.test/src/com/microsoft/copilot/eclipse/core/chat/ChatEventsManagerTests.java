// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;

class ChatEventsManagerTests {
  @Test
  void routesConcurrentProgressByTokenAndConversation() {
    ChatEventsManager manager = new ChatEventsManager();
    ChatProgressListener first = Mockito.mock(ChatProgressListener.class);
    ChatProgressListener second = Mockito.mock(ChatProgressListener.class);
    manager.registerProgressRoute("token-1", first);
    manager.registerProgressRoute("token-2", second);

    ChatProgressValue firstBegin = progress(WorkDoneProgressKind.begin, "conversation-1");
    ChatProgressValue secondBegin = progress(WorkDoneProgressKind.begin, "conversation-2");
    manager.notifyProgress("token-1", firstBegin);
    manager.notifyProgress("token-2", secondBegin);

    ChatProgressValue firstReport = progress(WorkDoneProgressKind.report, "conversation-1");
    manager.notifyProgress(null, firstReport);

    verify(first, times(1)).onChatProgress(firstBegin);
    verify(first, times(1)).onChatProgress(firstReport);
    verify(first, never()).onChatProgress(secondBegin);
    verify(second, times(1)).onChatProgress(secondBegin);
    verify(second, never()).onChatProgress(firstBegin);
    verify(second, never()).onChatProgress(firstReport);
  }

  @Test
  void unregisterRemovesTokenAndConversationAliases() {
    ChatEventsManager manager = new ChatEventsManager();
    ChatProgressListener listener = Mockito.mock(ChatProgressListener.class);
    manager.registerProgressRoute("token", listener);
    ChatProgressValue begin = progress(WorkDoneProgressKind.begin, "conversation");
    manager.notifyProgress("token", begin);

    manager.unregisterProgressRoutes(listener);
    manager.notifyProgress("token", progress(WorkDoneProgressKind.report, "conversation"));

    verify(listener, times(1)).onChatProgress(begin);
  }

  private static ChatProgressValue progress(WorkDoneProgressKind kind, String conversationId) {
    ChatProgressValue value = new ChatProgressValue();
    value.setKind(kind);
    value.setConversationId(conversationId);
    return value;
  }
}
