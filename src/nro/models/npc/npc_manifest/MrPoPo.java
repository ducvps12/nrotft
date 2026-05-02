package nro.models.npc.npc_manifest;

import clan.Clan;
import clan.ClanMember;
import consts.ConstNpc;
import models.DestronGas.DestronGas;
import models.DestronGas.DestronGasService;
import nro.models.npc.Npc;
import static nro.models.npc.NpcFactory.PLAYERID_OBJECT;
import nro.player.Player;
import nro.services.NpcService;
import nro.services.Service;
import services.func.Input;
import services.func.TopService;
import utils.TimeUtil;

public class MrPoPo extends Npc {

    public MrPoPo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        String message = "|7|━━ DESTRON GAS ━━\n\n"
                + "|1|Thượng Đế phát hiện 1 loại khí\n"
                + "|1|đang hủy diệt mọi mầm sống,\n"
                + "|1|nó được gọi là Destron Gas.\n\n"
                + "|2|★ Phần thưởng hoàn thành:\n"
                + "|8|• Vàng + Ngọc + Xu NRO\n"
                + "|8|• Lv20+: Thỏi vàng\n"
                + "|8|• Lv40+: Capsule dây chuyền\n"
                + "|8|• Lv60+: Sách kỹ năng, Hộp SKH\n"
                + "|8|• Lv80+: Mảnh BT, Sách TK2\n"
                + "|8|• Lv100+: Pet Po, Thú cưỡi\n"
                + "|8|• Clear Boss = x2 thưởng!\n\n"
                + "|7|━━━━━━━━━━━━━━━━━━━";

        if (player.clan != null) {
            // Hiển thị trạng thái bang hội
            String clanStatus = "";
            if (player.clan.KhiGasHuyDiet != null) {
                clanStatus = "\n|2|⚡ Bang đang tham gia Lv." + player.clan.KhiGasHuyDiet.level;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    message + clanStatus,
                    "Thông tin\nChi tiết", "Top 100\nBang hội",
                    "Thành tích\nBang", "OK", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    message + "\n|8|⚠ Cần gia nhập bang hội để tham gia!",
                    "Thông tin\nChi tiết", "Top 100\nBang hội",
                    "OK", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            if (player.clan != null) {
                // Có bang hội
                switch (select) {
                    case 0 -> showDetailedGuide(player);
                    case 1 -> TopService.gI().showTopClanKhiGas(player);
                    case 2 -> TopService.gI().showMyTopClanKhiGas(player);
                    case 3 -> handleJoinOrCreateDestronGas(player);
                    default -> { }
                }
            } else {
                // Không có bang hội
                switch (select) {
                    case 0 -> showDetailedGuide(player);
                    case 1 -> TopService.gI().showTopClanKhiGas(player);
                    case 2 -> NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Cần tham gia bang hội để sử dụng chức năng này!");
                    default -> {
                    }
                }
            }
        } else if (player.iDMark.getIndexMenu() == 2) {
            if (select == 0) {
                
                if (player.clan == null) {
                    return;
                }

                if (player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
                    NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Gia nhập bang hội trên 1 ngày mới được tham gia");
                    return;
                }
                
                if (player.clan.KhiGasHuyDiet == null) {
                    DestronGasService.gI().openKhiGasHuyDiet(
                            player,
                            Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id)))
                    );
                } else {
                    DestronGasService.gI().openKhiGasHuyDiet(player, (byte) 0);
                }
            }
        }
    }

    // ============ HƯỚNG DẪN CHI TIẾT DESTRON GAS ============
    private void showDetailedGuide(Player player) {
        NpcService.gI().createTutorial(player, tempId, this.avartar,
                "|7|━━ HƯỚNG DẪN DESTRON GAS ━━\n\n"
                + "|2|▶ Yêu cầu:\n"
                + "|8|• Có bang hội\n"
                + "|8|• Bang chủ mở phó bản\n"
                + "|8|• Tối đa 3 lượt/ngày/bang\n\n"
                + "|2|▶ Cách chơi:\n"
                + "|8|1) Chọn cấp độ (1-110)\n"
                + "|8|2) Cả bang vào đánh quái\n"
                + "|8|3) Hạ hết quái → Boss DrLychee\n"
                + "|8|4) Hạ DrLychee → Boss Hatchiyack\n"
                + "|8|5) Hạ Hatchiyack = hoàn thành!\n"
                + "|8|   Thời gian: 30 phút\n\n"
                + "|2|▶ Phần thưởng:\n"
                + "|8|• Hoàn thành: Vàng + Ngọc + Xu\n"
                + "|8|• Clear Boss: x2 thưởng cơ bản\n"
                + "|8|• Boss drop Cải trang mạnh\n"
                + "|8|• Lv cao: Item hiếm + thú cưỡi\n"
                + "|8|• Lv100 Clear: 0.5% Pet Po!");
    }

    private void handleJoinOrCreateDestronGas(Player player) {
        Clan clan = player.clan;
        if (clan == null) return;

        ClanMember cm = clan.getClanMember((int) player.id);
        if (cm == null) return;

        // Nếu bang hội đang tham gia
        if (clan.KhiGasHuyDiet != null) {
            createOtherMenu(player, 2,
                    "|7|━━ DESTRON GAS ĐANG MỞ ━━\n\n"
                            + "|1|Bang hội đang tham gia cấp độ |8|"
                            + clan.KhiGasHuyDiet.level + "\n"
                            + "|1|Đã mở: |8|"
                            + TimeUtil.convertTimeNow(clan.KhiGasHuyDiet.getLastTimeOpen())
                            + " trước\n\n"
                            + "|2|Cậu có muốn đi cùng họ không?\n"
                            + "|7|━━━━━━━━━━━━━━━━━━━",
                    "Đồng ý", "Từ chối");
            return;
        }

        // Chỉ bang chủ mới được mở
        if (!clan.isLeader(player)) {
            NpcService.gI().createTutorial(player, tempId, this.avartar,
                    "Chức năng chỉ dành cho bang chủ");
            return;
        }

        // Bang phải đủ thành viên
        if (clan.members.size() < DestronGas.N_PLAYER_CLAN) {
            NpcService.gI().createTutorial(player, tempId, this.avartar,
                    "Bang hội phải có ít nhất "
                            + DestronGas.N_PLAYER_CLAN
                            + " thành viên mới có thể tham gia");
            return;
        }

        // Mở form chọn cấp độ
        Input.gI().createFormChooseLevelKGHD(player);
    }
}
