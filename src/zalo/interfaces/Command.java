/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.interfaces;

import zalo.message.MessageContext;
import zalo.utils.Apis;
import java.util.List;
import java.util.Map;

public interface Command {

    String getName();

    String getDescription();

    String getTag();

    int getCooldown();

    int getRole();

    default boolean isHidden() {
        return false;
    }

    default String getUsage() {
        return null;
    }

    void run(CommandContext context);

    class CommandContext {
        private MessageContext message;
        private Apis api;
        private List<String> args;
        private Map<String, Command> commands;

        public MessageContext getMessage() {
            return message;
        }

        public void setMessage(MessageContext message) {
            this.message = message;
        }

        public Apis getApi() {
            return api;
        }

        public void setApi(Apis api) {
            this.api = api;
        }

        public List<String> getArgs() {
            return args;
        }

        public void setArgs(List<String> args) {
            this.args = args;
        }

        public Map<String, Command> getCommands() {
            return commands;
        }

        public void setCommands(Map<String, Command> commands) {
            this.commands = commands;
        }
    }
}
