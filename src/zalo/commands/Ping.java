/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;

public class Ping implements Command {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Kiểm tra độ trễ";
    }

    @Override
    public String getTag() {
        return "admin";
    }

    @Override
    public int getCooldown() {
        return 0;
    }

    @Override
    public int getRole() {
        return 2;
    }

    @Override
    public void run(Command.CommandContext context) {
        long startTime = System.currentTimeMillis();
        try {
            MessageContext message = context.getMessage();
            Apis api = context.getApi();

            String threadId = message.getThreadId();
            ThreadType threadType = message.getThreadType();

            long responseTime = System.currentTimeMillis() - startTime;
            String msg = "Pong!\nToc do phan hoi: " + responseTime + "ms";

            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            msgContent.setMsg(msg);
            msgContent.setQuote(SendMessageApi.createQuoteFromData(message.getData()));

            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
            System.err.println("[PING] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
