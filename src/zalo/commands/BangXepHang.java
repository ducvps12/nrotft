package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroBxhService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;

public class BangXepHang implements Command {

  @Override
  public String getName() {
    return "bxh";
  }

  @Override
  public String getDescription() {
    return "Xem bảng xếp hạng NRO: .bxh <method> [limit]\nMethods: sm, nv, whis, nap";
  }

  @Override
  public String getTag() {
    return "group";
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

      String threadId = message.getThreadId();
      ThreadType threadType = message.getThreadType();
      Map<String, Object> data = message.getData();

      if (!message.isGroup()) {
        SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
        msgContent.setMsg("Lệnh này chỉ dùng trong nhóm.");
        msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
        api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        return;
      }

      if (args == null || args.isEmpty()) {
        SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
        msgContent.setMsg("Sử dụng: .bxh <method> [limit]\n" +
            "Methods:\n" +
            "- sm: Top sức mạnh\n" +
            "- nv: Top nhiệm vụ\n" +
            "- whis: Top Whis\n" +
            "- nap: Top nạp\n" +
            "Ví dụ: .bxh sm 10");
        msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
        api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        return;
      }

      String method = args.get(0).trim().toLowerCase();
      if (!method.equals("sm") && !method.equals("nv") && !method.equals("whis") && !method.equals("nap")) {
        SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
        msgContent.setMsg("Method không hợp lệ. Sử dụng: sm, nv, whis, nap\nVí dụ: .bxh sm");
        msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
        api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        return;
      }

      int limit = 10;
      if (args.size() > 1) {
        try {
          limit = Integer.parseInt(args.get(1).trim());
          if (limit < 1 || limit > 100) {
            limit = 10;
          }
        } catch (NumberFormatException e) {
          limit = 10;
        }
      }

      JSONObject result = NroBxhService.gI().getBxh(method, limit);

      SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
      if (result == null) {
        msgContent.setMsg("Không thể lấy dữ liệu BXH từ database.");
      } else {
        JSONArray dataArray = (JSONArray) result.get("data");
        if (dataArray == null || dataArray.isEmpty()) {
          msgContent.setMsg("Không có dữ liệu BXH.");
        } else {
          StringBuilder sb = new StringBuilder();
          String title = getTitle(method);
          sb.append("🏆 ").append(title).append(" - Top ").append(limit).append("\n\n");

          for (int i = 0; i < dataArray.size(); i++) {
            JSONObject item = (JSONObject) dataArray.get(i);
            int rank = ((Long) item.get("rank")).intValue();
            String name = (String) item.get("name");
            String value = getValueString(method, item);

            sb.append(rank).append(". ").append(name);
            if (value != null && !value.isEmpty()) {
              sb.append(" - ").append(value);
            }
            sb.append("\n");
          }

          msgContent.setMsg(sb.toString());
        }
      }
      msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
      api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
    } catch (Exception e) {
    }
  }

  private String getTitle(String method) {
    switch (method) {
      case "sm":

        return "Bảng Xếp Hạng Sức Mạnh";
      case "nv":

        return "Bảng Xếp Hạng Nhiệm Vụ";
      case "whis":

        return "Bảng Xếp Hạng Whis";
      case "nap":

        return "Bảng Xếp Hạng Nạp";
      default:

        return "Bảng Xếp Hạng";
    }
  }

  private String getValueString(String method, JSONObject item) {
    switch (method) {
      case "sm":
        Object powerObj = item.get("power");

        long power;
        if (powerObj instanceof Long) {
          power = (Long) powerObj;
        } else if (powerObj instanceof Integer) {
          power = ((Integer) powerObj).longValue();

        } else if (powerObj instanceof Number) {
          power = ((Number) powerObj).longValue();
        } else {
          power = 0;
        }
        return formatPower(power) + " SM";

      case "nv":
        Byte nv = ((Long) item.get("nv")).byteValue();
        Byte subnv = ((Long) item.get("subnv")).byteValue();
        return "NV " + nv + "-" + subnv;
      case "whis":
        Integer level = ((Long) item.get("level")).intValue();

        Integer time = ((Long) item.get("time")).intValue();
        return "LV " + level + " - " + (time / 1000) + "s";
      case "nap":
        Object cashObj = item.get("cash");
        long cash;
        if (cashObj instanceof Long) {
          cash = (Long) cashObj;
        } else if (cashObj instanceof Integer) {
          cash = ((Integer) cashObj).longValue();
        } else if (cashObj instanceof Number) {
          cash = ((Number) cashObj).longValue();
        } else {
          cash = 0;
        }

        return formatMoney(cash) + " VNĐ";
      default:
        return "";
    }
  }

  private String formatPower(long power) {
    java.text.NumberFormat num = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
    num.setMaximumFractionDigits(1);
    if (power >= 1_000_000_000L) {
      return num.format((double) power / 1_000_000_000.0) + " Tỷ";
    } else if (power >= 1_000_000L) {
      return num.format((double) power / 1_000_000.0) + " Tr";
    } else if (power >= 1_000L) {
      return num.format((double) power / 1_000.0) + " k";
    } else {
      return num.format(power);
    }
  }

  private String formatMoney(long money) {
    java.text.NumberFormat num = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
    num.setMaximumFractionDigits(1);
    if (money >= 1_000_000_000L) {
      return num.format((double) money / 1_000_000_000.0) + " Tỷ";
    } else if (money >= 1_000_000L) {
      return num.format((double) money / 1_000_000.0) + " Tr";
    } else if (money >= 1_000L) {
      return num.format((double) money / 1_000.0) + " k";
    } else {
      return num.format(money);
    }
  }
}
// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT

// ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
