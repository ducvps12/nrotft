/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import java.util.*;

public class Help implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Hiển thị danh sách lệnh";
    }

    @Override
    public String getTag() {
        return "ALL";
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
        try {
            MessageContext message = context.getMessage();
            Apis api = context.getApi();
            List<String> args = context.getArgs();
            Map<String, Command> commands = context.getCommands();

            String threadId = message.getThreadId();
            ThreadType threadType = message.getThreadType();

            if (args != null && !args.isEmpty()) {
                String query = String.join(" ", args).toLowerCase();
                Command found = commands.get(query);

                if (found == null || found.isHidden()) {
                    api.sendMessage.sendMessage(
                            "Không tìm thấy lệnh: " + query,
                            threadId,
                            threadType).get();
                    return;
                }

                List<String> lines = new ArrayList<>();
                lines.add("LỆNH: " + found.getName());
                if (found.getDescription() != null) {
                    lines.add("Mô tả: " + found.getDescription());
                }
                if (found.getUsage() != null) {
                    lines.add("Cách dùng: " + found.getUsage());
                }
                lines.add("Danh mục: " + found.getTag());
                lines.add("Quyền: " + found.getRole());
                if (found.getCooldown() > 0) {
                    lines.add("Cooldown: " + found.getCooldown() + "s");
                }

                api.sendMessage.sendMessage(
                        String.join("\n", lines),
                        threadId,
                        threadType).get();
                return;
            }

            Map<String, List<Command>> byTag = new HashMap<>();
            for (Command cmd : commands.values()) {
                if (cmd.isHidden() || cmd.getName().equalsIgnoreCase("help")) {
                    continue;
                }
                String tag = cmd.getTag().toUpperCase();
                byTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(cmd);
            }

            List<String> tagKeys = new ArrayList<>(byTag.keySet());
            Collections.sort(tagKeys);
            for (List<Command> cmdList : byTag.values()) {
                cmdList.sort(Comparator.comparing(Command::getName));
            }

            List<String> lines = new ArrayList<>();
            lines.add("DANH MỤC LỆNH");
            for (String tag : tagKeys) {
                List<String> names = new ArrayList<>();
                for (Command cmd : byTag.get(tag)) {
                    names.add(cmd.getName());
                }
                lines.add("[ " + tag + " ]");
                lines.add(String.join(", ", names));
                lines.add("");
            }
            lines.add("Chi tiết: .help <tên_lệnh>");

            api.sendMessage.sendMessage(
                    String.join("\n", lines),
                    threadId,
                    threadType).get();

        } catch (Exception e) {
            System.err.println("[HELP] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
