package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import models.The23rdMartialArtCongress.The23rdMartialArtCongressService;
import models.WorldMartialArtsTournament.WorldMartialArtsTournamentService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Manager;
import nro.server.ServerNotify;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.PlayerService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class GhiDanh extends Npc {

    String[] menuselect = new String[] {};

    public GhiDanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            switch (this.mapId) {
                case 42, 43, 44 -> {
                    int pts = pl.point_maydam;
                    int nextMilestone = getNextMilestone(pts);
                    String milestoneInfo = nextMilestone > 0
                            ? "\nMốc tiếp: " + nextMilestone + " điểm"
                            : "\n★ Đã đạt tất cả mốc thưởng!";
                    this.createOtherMenu(pl, ConstNpc.MAY_DAM,
                            "|7|━━ MÁY ĐO SỨC MẠNH ━━\n\n"
                                    + "|1|Điểm hiện tại: |8|" + pts + milestoneInfo + "\n\n"
                                    + "|2|★ Phần thưởng mốc:\n"
                                    + "|8|• 100đ: 2 Xu NRO + 20M vàng\n"
                                    + "|8|• 500đ: 50M vàng + 10 ngọc\n"
                                    + "|8|• 1000đ: 1 Thỏi vàng + 20 ngọc\n"
                                    + "|8|• 5000đ: 3 Thỏi vàng + 50 ngọc\n"
                                    + "|8|• 10000đ: 5 Thỏi vàng + 100 ngọc",
                            "Top 100\nTrái đất",
                            "Top 100\nNamek",
                            "Top 100\nXayda",
                            "Xem điểm\n& Thưởng",
                            "Đóng");
                }
                case 52 ->
                    WorldMartialArtsTournamentService.menu(this, pl);
                case 129 -> {
                    if (Util.isAfterMidnight(pl.lastTimePKDHVT23)) {
                        pl.goldChallenge = 50_000_000;
                        pl.rubyChallenge = 100;
                        pl.levelWoodChest = 0;
                    }
                    long goldchallenge = pl.goldChallenge;
                    long rubychallenge = pl.rubyChallenge;
                    if (pl.levelWoodChest == 0) {
                        menuselect = new String[] { "Hướng\ndẫn\nthêm",
                                "Thi đấu\n" + Util.numberToMoney(rubychallenge) + " hồng ngọc",
                                "Thi đấu\n" + Util.numberToMoney(goldchallenge) + " vàng", "Về\nĐại Hội\nVõ Thuật" };
                    } else {
                        menuselect = new String[] { "Hướng\ndẫn\nthêm",
                                "Thi đấu\n" + Util.numberToMoney(rubychallenge) + " hồng ngọc",
                                "Thi đấu\n" + Util.numberToMoney(goldchallenge) + " vàng",
                                "Nhận\nthưởng\nRương Cấp\n" + pl.levelWoodChest, "Về\nĐại Hội\nVõ Thuật" };
                    }
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                            "|7|━━ ĐẠI HỘI VÕ THUẬT 23 ━━\n\n"
                                    + "|1|Round hiện tại: |8|" + pl.levelWoodChest + " / 12\n\n"
                                    + "|2|★ Thưởng mốc đặc biệt:\n"
                                    + "|8|• Round 4: 50M vàng + 10 ngọc\n"
                                    + "|8|• Round 8: 3 Thỏi vàng + 30 ngọc\n"
                                    + "|8|• Round 12: JACKPOT + Thông báo!\n\n"
                                    + "|8|Chi phí: " + Util.numberToMoney(goldchallenge) + " vàng"
                                    + " hoặc " + Util.numberToMoney(rubychallenge) + " hồng ngọc",
                            menuselect, "Từ chối");
                }
                default ->
                    super.openBaseMenu(pl);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 52) {
                WorldMartialArtsTournamentService.confirm(this, player, select);
            } else if (this.mapId == 129) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU -> {
                        long goldchallenge = player.goldChallenge;
                        long rubychallenge = player.rubyChallenge;
                        if (player.levelWoodChest == 0) {
                            switch (select) {
                                case 0 ->
                                    NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.NPC_DHVT23);
                                case 1, 2 -> {
                                    if (player.levelWoodChest != 12) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (select == 1) {
                                                if (player.inventory.ruby >= rubychallenge) {
                                                    The23rdMartialArtCongressService.gI().startChallenge(player);
                                                    player.inventory.ruby -= (rubychallenge);
                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                    player.goldChallenge += 50000000;
                                                    player.rubyChallenge += 100;
                                                } else {
                                                    Service.gI().sendThongBao(player,
                                                            "Bạn không đủ hồng ngọc, còn thiếu "
                                                                    + Util.numberToMoney(
                                                                            rubychallenge - player.inventory.ruby)
                                                                    + " hồng ngọc nữa");
                                                }
                                            } else {
                                                if (player.inventory.gold >= goldchallenge) {
                                                    The23rdMartialArtCongressService.gI().startChallenge(player);
                                                    player.inventory.gold -= (goldchallenge);
                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                    player.goldChallenge += 50000000;
                                                    player.rubyChallenge += 100;
                                                } else {
                                                    Service.gI().sendThongBao(player,
                                                            "Bạn không đủ vàng, còn thiếu "
                                                                    + Util.numberToMoney(
                                                                            goldchallenge - player.inventory.gold)
                                                                    + " vàng nữa");
                                                }
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hãy mở rương báu vật trước");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player,
                                                "Bạn đã vô địch giải. Vui lòng chờ đến ngày mai");
                                    }
                                }
                                case 3 ->
                                    ChangeMapService.gI().changeMapNonSpaceship(player, 52, player.location.x, 336);
                            }
                        } else {
                            switch (select) {
                                case 0 ->
                                    NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.NPC_DHVT23);
                                case 1, 2 -> {
                                    if (player.levelWoodChest != 12) {
                                        if (InventoryService.gI().finditemWoodChest(player)) {
                                            if (select == 1) {
                                                if (player.inventory.ruby >= rubychallenge) {
                                                    The23rdMartialArtCongressService.gI().startChallenge(player);
                                                    player.inventory.ruby -= (rubychallenge);
                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                    player.goldChallenge += 50000000;
                                                    player.rubyChallenge += 100;
                                                } else {
                                                    Service.gI().sendThongBao(player,
                                                            "Bạn không đủ hồng ngọc, còn thiếu "
                                                                    + Util.numberToMoney(
                                                                            rubychallenge - player.inventory.ruby)
                                                                    + " hồng ngọc nữa");
                                                }
                                            } else {
                                                if (player.inventory.gold >= goldchallenge) {
                                                    The23rdMartialArtCongressService.gI().startChallenge(player);
                                                    player.inventory.gold -= (goldchallenge);
                                                    PlayerService.gI().sendInfoHpMpMoney(player);
                                                    player.goldChallenge += 50000000;
                                                    player.rubyChallenge += 100;
                                                } else {
                                                    Service.gI().sendThongBao(player,
                                                            "Bạn không đủ vàng, còn thiếu "
                                                                    + Util.numberToMoney(
                                                                            goldchallenge - player.inventory.gold)
                                                                    + " vàng nữa");
                                                }
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hãy mở rương báu vật trước");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player,
                                                "Bạn đã vô địch giải. Vui lòng chờ đến ngày mai");
                                    }
                                }
                                case 3 ->
                                    this.createOtherMenu(player, 1,
                                            "Phần thưởng của bạn đang ở cấp " + player.levelWoodChest + " / 12\n"
                                                    + "Mỗi ngày chỉ được nhận được nhận thưởng 1 lần\n"
                                                    + "bạn có chắc sẽ nhận phần thưởng ngay bây giờ?",
                                            "OK", "Từ chối");
                                case 4 ->
                                    ChangeMapService.gI().changeMapNonSpaceship(player, 52, player.location.x, 336);
                            }
                        }
                    }
                    case 1 -> {
                        if (select == 0) {
                            if (InventoryService.gI().finditemWoodChest(player)) {
                                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                    Item it = ItemService.gI().createNewItem((short) 570);
                                    it.itemOptions.add(new Item.ItemOption(72, player.levelWoodChest));
                                    it.itemOptions.add(new Item.ItemOption(30, 0));
                                    it.createTime = System.currentTimeMillis();
                                    InventoryService.gI().addItemBag(player, it);
                                    InventoryService.gI().sendItemBag(player);
                                    player.levelWoodChest = 0;
                                    player.lastTimeRewardWoodChest = System.currentTimeMillis();
                                    NpcService.gI().createMenuConMeo(player, -1, -1,
                                            "Bạn nhận được\n|1|Rương Gỗ\n|2|Giấu bên trong nhiều vật phẩm quý giá",
                                            "OK");
                                } else {
                                    this.npcChat(player,
                                            "Hành trang đã đầy, cần một ô trống trong hành trang để nhận vật phẩm");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Hãy mở rương báu vật trước");
                            }
                        }
                    }
                }
            } else if (this.mapId == 44 || this.mapId == 43 || this.mapId == 42) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.MAY_DAM -> {
                        switch (select) {
                            case 0 ->
                                Service.gI().showListTopTraiDat(player, Manager.Topmaydam);
                            case 1 ->
                                Service.gI().showListTopNamek(player, Manager.Topmaydam);
                            case 2 ->
                                Service.gI().showListTopXayda(player, Manager.Topmaydam);
                            case 3 -> handleMaydamReward(player);
                        }
                    }
                    case 2010 -> {
                        if (select == 0) {
                            claimMaydamMilestone(player);
                        }
                    }
                }
            }
        }
    }

    // ============ PHẦN THƯỞNG MỐC MÁY ĐẤM ============
    private static final int[] MILESTONES = {100, 500, 1000, 5000, 10000};
    private static final String[] MILESTONE_REWARDS = {
        "2 Xu NRO + 20M vàng",
        "50M vàng + 10 ngọc",
        "1 Thỏi vàng + 20 ngọc",
        "3 Thỏi vàng + 50 ngọc",
        "5 Thỏi vàng + 100 ngọc"
    };

    private void handleMaydamReward(Player player) {
        int pts = player.point_maydam;
        int claimed = player.claimedMaydamMilestone; // bitmask

        StringBuilder sb = new StringBuilder();
        sb.append("|7|━━ ĐIỂM MÁY ĐẤM ━━\n\n");
        sb.append("|1|Điểm hiện tại: |8|").append(pts).append("\n\n");
        sb.append("|2|Trạng thái mốc thưởng:\n");

        boolean hasUnclaimedMilestone = false;
        for (int i = 0; i < MILESTONES.length; i++) {
            int m = MILESTONES[i];
            boolean reached = pts >= m;
            boolean alreadyClaimed = (claimed & (1 << i)) != 0;

            if (reached && alreadyClaimed) {
                sb.append("|8|✅ ").append(m).append("đ: ").append(MILESTONE_REWARDS[i]).append(" (ĐÃ NHẬN)\n");
            } else if (reached) {
                sb.append("|2|🎁 ").append(m).append("đ: ").append(MILESTONE_REWARDS[i]).append(" (CHƯA NHẬN)\n");
                hasUnclaimedMilestone = true;
            } else {
                sb.append("|1|⬜ ").append(m).append("đ: ").append(MILESTONE_REWARDS[i]).append("\n");
            }
        }

        if (hasUnclaimedMilestone) {
            this.createOtherMenu(player, 2010, sb.toString(), "Nhận\nthưởng mốc", "Đóng");
        } else {
            int nextMilestone = getNextMilestone(pts);
            if (nextMilestone > 0) {
                sb.append("\n|1|Còn |8|").append(nextMilestone - pts).append("|1| điểm nữa!");
            } else {
                sb.append("\n|2|★ Đã nhận hết tất cả mốc thưởng!");
            }
            this.createOtherMenu(player, 2010, sb.toString(), "Đóng");
        }
    }

    private int getNextMilestone(int pts) {
        for (int m : MILESTONES) {
            if (pts < m) return m;
        }
        return 0;
    }

    // Xử lý nhận thưởng mốc máy đấm (menu 2010, select 0)
    private void claimMaydamMilestone(Player player) {
        int pts = player.point_maydam;
        int claimed = player.claimedMaydamMilestone;

        // Chống spam bấm liên tục
        if (System.currentTimeMillis() - player.lastTimeClaimMaydam < 5000) {
            Service.gI().sendThongBao(player, "Vui lòng chờ 5 giây!");
            return;
        }

        // Kiểm tra hành trang trống
        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3 ô trống trong hành trang!");
            return;
        }

        // Tìm và nhận tất cả mốc chưa claimed
        StringBuilder reward = new StringBuilder();
        int xuTotal = 0;
        long goldTotal = 0;
        int gemTotal = 0;
        boolean anyNewClaim = false;

        // Mốc 100: 2 Xu NRO + 20M vàng
        if (pts >= 100 && (claimed & (1 << 0)) == 0) {
            xuTotal += 2;
            goldTotal += 20_000_000L;
            claimed |= (1 << 0);
            anyNewClaim = true;
        }
        // Mốc 500: 50M vàng + 10 ngọc
        if (pts >= 500 && (claimed & (1 << 1)) == 0) {
            goldTotal += 50_000_000L;
            gemTotal += 10;
            claimed |= (1 << 1);
            anyNewClaim = true;
        }
        // Mốc 1000: 1 Thỏi vàng + 20 ngọc
        if (pts >= 1000 && (claimed & (1 << 2)) == 0) {
            gemTotal += 20;
            Item tv = ItemService.gI().createNewItem((short) 457, 1);
            tv.itemOptions.add(new Item.ItemOption(30, 0));
            InventoryService.gI().addItemBag(player, tv);
            reward.append(", 1 Thỏi vàng");
            claimed |= (1 << 2);
            anyNewClaim = true;
        }
        // Mốc 5000: 3 Thỏi vàng + 50 ngọc
        if (pts >= 5000 && (claimed & (1 << 3)) == 0) {
            gemTotal += 50;
            Item tv = ItemService.gI().createNewItem((short) 457, 3);
            tv.itemOptions.add(new Item.ItemOption(30, 0));
            InventoryService.gI().addItemBag(player, tv);
            reward.append(", 3 Thỏi vàng");
            claimed |= (1 << 3);
            anyNewClaim = true;
        }
        // Mốc 10000: 5 Thỏi vàng + 100 ngọc
        if (pts >= 10000 && (claimed & (1 << 4)) == 0) {
            gemTotal += 100;
            Item tv = ItemService.gI().createNewItem((short) 457, 5);
            tv.itemOptions.add(new Item.ItemOption(30, 0));
            InventoryService.gI().addItemBag(player, tv);
            reward.append(", 5 Thỏi vàng");
            claimed |= (1 << 4);
            anyNewClaim = true;
            ServerNotify.gI().notify("★ " + player.name + " đạt 10.000 điểm Máy Đấm!");
        }

        if (!anyNewClaim) {
            Service.gI().sendThongBao(player, "Không có mốc thưởng nào để nhận!");
            return;
        }

        // Cộng phần thưởng
        if (xuTotal > 0) {
            Item xu = ItemService.gI().createNewItem((short) 1705, xuTotal);
            InventoryService.gI().addItemBag(player, xu);
        }
        player.inventory.addGoldSafe(goldTotal);
        player.inventory.gem += gemTotal;

        // Lưu bitmask đã claimed (KHÔNG reset điểm)
        player.claimedMaydamMilestone = claimed;
        player.lastTimeClaimMaydam = System.currentTimeMillis();

        InventoryService.gI().sendItemBag(player);
        PlayerService.gI().sendInfoHpMpMoney(player);
        Service.gI().sendThongBao(player, "★ Nhận thưởng mốc Máy Đấm!\n"
                + xuTotal + " Xu NRO, " + gemTotal + " ngọc, "
                + Util.numberToMoney(goldTotal) + " vàng" + reward);
    }
}

