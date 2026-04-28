package nro.server;

/*
 *
 *
 *  Box ZALO:
 *  sdt zalo: 0376263452
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import boss.boss_manifest.LuyenTap.NPC_MrPopo;
import models.Card.OptionCard;
import models.Card.RadarService;
import models.Card.RadarCard;
import jdbc.DBConnecter;
import consts.ConstPlayer;
import consts.ConstMap;
import consts.ConstNpc;
import data.DataGame;
import jdbc.daos.ShopDAO;
import models.Template.*;
import clan.Clan;
import clan.ClanMember;
import consts.ConstDataEventCHUCVIP;
import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;
import consts.ConstDataEventTOP;
import consts.ConstDataEventTRANGSUCVIP;
import consts.ConstDataEventthangmuoi;
import consts.ConstSQL;

import static data.DataGame.MAP_MOUNT_NUM;
import encrypt.ImageUtil;

import models.GiftCode.GiftCode;
import models.GiftCode.GiftCodeManager;
import intrinsic.Intrinsic;
import item.CaiTrang;
import item.Item;
import item.Item.ItemOption;
import map.WayPoint;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import player.badges.BagesTemplate;
import shop.Shop;
import skill.NClass;
import skill.Skill;
import task.Badges.BadgesTaskTemplate;
import task.SideTaskTemplate;
import task.SubTaskMain;
import task.TaskMain;
import nro.services.ItemService;
import nro.services.MapService;
import utils.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import map.EffectEventManager;
import map.EffectMap;

import matches.TOP;
import models.kygui.ConsignItem;
import models.kygui.ConsignShopManager;
import utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.models.npc.NonInteractiveNPC;
import nro.tambao.TamBaoService;
import power.CaptionManager;
import power.PowerLimitManager;
import task.ClanTaskTemplate;
import task.KolTaskTemplate;

public final class Manager {

    private static Manager instance;

    public static String apiKey = "abcdef";
    public static int workerGroup = 10;
    public static String executeCommand;
    public static boolean debug;
    public static int apiPort = 8899;

    public static byte SERVER = 1;
    public static byte SECOND_WAIT_LOGIN = 5;
    public static int MAX_PER_IP = 10;
    public static int MAX_PLAYER = 2000;
    public static int EVENT_SEVER = 0;
    public static double RATE_EXP_SERVER = 1.0;
    public static boolean LOCAL = false;
    public static boolean TEST = false;
    public static boolean DAO_AUTO_UPDATER = false;
    // Lưu trữ cấu hình phần thưởng Boss từ Panel: Key = BossID, Value = Chuỗi ID
    // vật phẩm (vd: "457,1229")
    public static final Map<Integer, String> BOSS_REWARD_PANEL = new HashMap<>();
    public static final List<KolTaskTemplate> KOL_TASKS_TEMPLATE = new ArrayList<>();
    public static MapTemplate[] MAP_TEMPLATES;
    public static final List<map.Map> MAPS = new ArrayList<>();
    public static final List<ItemOptionTemplate> ITEM_OPTION_TEMPLATES = new ArrayList<>();
    public static final List<ArrHead2Frames> ARR_HEAD_2_FRAMES = new ArrayList<>();
    public static final Map<String, Byte> IMAGES_BY_NAME = new HashMap<>();
    public static final List<ItemTemplate> ITEM_TEMPLATES = new ArrayList<>();
    public static final List<MobTemplate> MOB_TEMPLATES = new ArrayList<>();
    public static final List<NpcTemplate> NPC_TEMPLATES = new ArrayList<>();
    public static final List<TaskMain> TASKS = new ArrayList<>();
    public static final List<SideTaskTemplate> SIDE_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<ClanTaskTemplate> CLAN_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<AchievementTemplate> ACHIEVEMENT_TEMPLATE = new ArrayList<>();
    public static int AUTO_MAINTENANCE = 1;
    public static int AUTO_MAINTENANCE_HOUR = 1;
    public static int AUTO_MAINTENANCE_MINUTE = 1;
    public static boolean LUNNAR_NEW_YEAR = false;
    public static boolean INTERNATIONAL_WOMANS_DAY = false;
    public static boolean CHRISTMAS = false;
    public static boolean HALLOWEEN = false;
    public static boolean HUNG_VUONG = false;
    public static boolean TRUNG_THU = false;
    public static boolean TOP_UP = false;

    public static final List<Intrinsic> INTRINSICS = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_TD = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_NM = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_XD = new ArrayList<>();
    public static final List<HeadAvatar> HEAD_AVATARS = new ArrayList<>();
    public static final List<BgItem> BG_ITEMS = new ArrayList<>();
    public static final List<FlagBag> FLAGS_BAGS = new ArrayList<>();
    public static final List<NClass> NCLASS = new ArrayList<>();
    public static final List<Npc> NPCS = new ArrayList<>();
    public static List<Shop> SHOPS = new ArrayList<>();
    public static final List<Clan> CLANS = new ArrayList<>();
    public static final List<String> NOTIFY = new ArrayList<>();
    public static final List<BadgesTaskTemplate> TASKS_BADGES_TEMPLATE = new ArrayList<>();
    public static final List<BagesTemplate> BAGES_TEMPLATES = new ArrayList<>();
    public static final List<CaiTrang> CAI_TRANGS = new ArrayList<>();
    public static boolean isTopMaydamChanged = false;
    public static boolean isTopWhisChanged = false;
    public static List<TOP> Topmaydam;

    public static final short[] itemIds_Kaio_AWJ = { 232, 236, 240, 244, 248, 252, 268, 272, 276 };
    public static final short[] itemIds_tl_AWJ = { 555, 557, 559, 556, 558, 560, 563, 565, 567 };
    public static final short[] itemIds_tl_GN = { 562, 564, 566, 561 };
    public static final short[] itemIds_Kaio_GN = { 256, 260, 264, 280 };
    public static final short[] itemIds_LuongLong_AWJ = { 233, 237, 241, 245, 249, 253, 269, 273, 277 };
    public static final short[] itemIds_LuongLong_GN = { 257, 261, 265, 281 };

    public static final short[] aotd = { 138, 139, 230, 231, 232, 233, 555 };
    public static final short[] quantd = { 142, 143, 242, 243, 244, 245, 556 };
    public static final short[] gangtd = { 146, 147, 254, 255, 256, 257, 562 };
    public static final short[] giaytd = { 150, 151, 266, 267, 268, 269, 563 };
    public static final short[] aoxd = { 170, 171, 238, 239, 240, 241, 559 };
    public static final short[] quanxd = { 174, 175, 250, 251, 252, 253, 560 };
    public static final short[] gangxd = { 178, 179, 262, 263, 264, 265, 566 };
    public static final short[] giayxd = { 182, 183, 274, 275, 276, 277, 567 };
    public static final short[] aonm = { 154, 155, 234, 235, 236, 237, 557 };
    public static final short[] quannm = { 158, 159, 246, 247, 248, 249, 558 };
    public static final short[] gangnm = { 162, 163, 258, 259, 260, 261, 564 };
    public static final short[] giaynm = { 166, 167, 270, 271, 272, 273, 565 };
    public static final short[] radaSKHVip = { 186, 187, 278, 279, 280, 281, 561 };
    public static final short[][][] doSKHVip = { { aotd, quantd, gangtd, giaytd }, { aonm, quannm, gangnm, giaynm },
            { aoxd, quanxd, gangxd, giayxd } };

    public static List<TOP> topSM;
    public static List<TOP> topNap;
    public static List<TOP> topDuaSM;
    public static List<TOP> topDuaNap;
    public static List<TOP> topSD;
    public static List<TOP> topHP;
    public static List<TOP> topKI;
    public static List<TOP> topNV;
    public static List<TOP> topSK;
    public static List<TOP> topPVP;
    public static List<TOP> topNHS;
    public static List<TOP> topDC;
    public static List<TOP> topVDST;
    public static List<TOP> topWHIS;
    public static long timeRealTop = 0;
    public static final short[][] trangBiKichHoat = { { 0, 6, 21, 27 }, { 1, 7, 22, 28 }, { 2, 8, 23, 29 } };
    public static final short[][] trangBiKichHoatVip = { { 555, 556, 562, 563 }, { 557, 558, 564, 565 },
            { 559, 560, 566, 567 } };

    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static boolean hasNewTopScores() {
        return isTopMaydamChanged || isTopWhisChanged;
    }

    public static void resetTopFlags() {
        isTopMaydamChanged = false;
        isTopWhisChanged = false;
    }

    private Manager() {
        try {
            loadProperties();
        } catch (IOException ex) {
            Logger.logException(Manager.class, ex, "Lỗi load properites");
            System.exit(0);
        }
        ImageUtil.initImage();
        // TamBaoService.loadItem();
        this.loadDatabase();
        NpcFactory.createNpcConMeo();
        NpcFactory.createNpcRongThieng();
        this.initMap();
        System.out.println("Finish connect Server: " + DBConnecter.DB_DATA);
    }

    private void initMap() {
        int[][] tileTypeTop = readTileIndexTileType(ConstMap.TILE_TOP);

        for (MapTemplate mapTemp : MAP_TEMPLATES) {
            int[][] tileMap = readTileMap(mapTemp.id);
            int[] tileTop = tileTypeTop[mapTemp.tileId - 1];

            map.Map map = new map.Map(
                    mapTemp.id,
                    mapTemp.name,
                    mapTemp.planetId,
                    mapTemp.tileId,
                    mapTemp.bgId,
                    mapTemp.bgType,
                    mapTemp.type,
                    tileMap,
                    tileTop,
                    mapTemp.zones,
                    mapTemp.isMapOffline(),
                    mapTemp.maxPlayerPerZone,
                    mapTemp.wayPoints,
                    mapTemp.effectMaps);

            MAPS.add(map);
            map.initMob(mapTemp.mobTemp, mapTemp.mobLevel, mapTemp.mobHp, mapTemp.mobX, mapTemp.mobY);
            map.initNpc(mapTemp.npcId, mapTemp.npcX, mapTemp.npcY);
            
            // Spawn GohanUltra ở 3 map đầu có SKH: map 1 (Trái Đất), map 8 (Namếc), map 15 (Xayda)
            // Mỗi map chỉ 1 NPC duy nhất - không spam spawn
            if (map.mapId == 1 || map.mapId == 8 || map.mapId == 15) {
                short x = 420;
                short y = (short) map.yPhysicInTop(x, 100);
                if (y <= 0) {
                    y = 336;
                }
                map.npcs.add(NpcFactory.createNPC(map.mapId, 1, x, y, ConstNpc.GOHAN_ULTRA));
            }
            
            // Spawn Mr.PoPo ở Thần Điện (map 45) - đứng bên trái, xa Thượng Đế
            if (map.mapId == ConstMap.THAN_DIEN) {
                map.npcs.add(NpcFactory.createNPC(map.mapId, 1, 200, 408, ConstNpc.MR_POPO));
            }
            
            // Spawn NPC Bảng Danh Vọng ở Sân Vườn (map 131-133) để tân thủ thấy ngay khi ra khỏi nhà
            if (map.mapId >= 131 && map.mapId <= 133) {
                short x = 300;
                short y = (short) map.yPhysicInTop(x, 100);
                if (y <= 0) {
                    y = 336;
                }
                map.npcs.add(NpcFactory.createNPC(map.mapId, 1, x, y, ConstNpc.BANG_DANH_VONG));
            }
            
            // Spawn GohanUltra ở map đầu Majin (map 199) cho SKH
            if (map.mapId == ConstMap.HOANG_MAC_MAJIN) {
                short x = 420;
                short y = (short) map.yPhysicInTop(x, 100);
                if (y <= 0) {
                    y = 336;
                }
                map.npcs.add(NpcFactory.createNPC(map.mapId, 1, x, y, ConstNpc.GOHAN_ULTRA));
            }

            // Dùng Virtual Thread để update map
            Thread.startVirtualThread(() -> map.run());
        }

        new NonInteractiveNPC().initNonInteractiveNPC();
        NPC_MrPopo popo = new NPC_MrPopo();
        popo.initNPC_MrPopo();
        Logger.log("Initialize map successfully!\n");
    }

    private void loadDatabase() {
        long st = System.currentTimeMillis();
        JSONArray dataArray;
        JSONObject dataObject;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection con2 = DBConnecter.getConnectionServer();) {
            // load clan
            ps = con2.prepareStatement("select * from clan");
            rs = ps.executeQuery();
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getInt("id");
                clan.name = rs.getString("name");
                clan.name2 = rs.getString("name_2");
                clan.slogan = rs.getString("slogan");
                clan.imgId = rs.getByte("img_id");
                clan.powerPoint = rs.getLong("power_point");
                clan.maxMember = rs.getByte("max_member");
                clan.capsuleClan = rs.getInt("clan_point");
                clan.level = rs.getByte("level");
                if (clan.level < 1) {
                    clan.level = 1;
                }
                clan.createTime = (int) (rs.getTimestamp("create_time").getTime() / 1000);
                dataArray = (JSONArray) JSONValue.parse(rs.getString("members"));
                for (int i = 0; i < dataArray.size(); i++) {
                    dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    ClanMember cm = new ClanMember();
                    cm.clan = clan;
                    cm.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                    cm.name = String.valueOf(dataObject.get("name"));
                    cm.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                    cm.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                    cm.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                    cm.role = Byte.parseByte(String.valueOf(dataObject.get("role")));
                    cm.donate = Integer.parseInt(String.valueOf(dataObject.get("donate")));
                    cm.receiveDonate = Integer.parseInt(String.valueOf(dataObject.get("receive_donate")));
                    cm.memberPoint = Integer.parseInt(String.valueOf(dataObject.get("member_point")));
                    cm.clanPoint = Integer.parseInt(String.valueOf(dataObject.get("clan_point")));
                    cm.joinTime = Integer.parseInt(String.valueOf(dataObject.get("join_time")));
                    cm.timeAskPea = Long.parseLong(String.valueOf(dataObject.get("ask_pea_time")));
                    try {
                        cm.powerPoint = Long.parseLong(String.valueOf(dataObject.get("power")));
                    } catch (NumberFormatException e) {
                    }
                    clan.addClanMember(cm);
                }
                dataArray.clear();
                CLANS.add(clan);
            }

            ps = con2.prepareStatement("select id from clan order by id desc limit 1");
            rs = ps.executeQuery();
            if (rs.next()) {
                Clan.NEXT_ID = rs.getInt("id") + 1;
            }

            Logger.log("Downloaded Loading clan (" + CLANS.size() + "), clan next id: " + Clan.NEXT_ID + "\n");

            // Load giftcode
            ps = con2.prepareStatement("SELECT * FROM giftcode");
            rs = ps.executeQuery();
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (int i = 0; i < jar.size(); ++i) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);

                        int id = Integer.parseInt(jsonObj.get("temp_id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());

                        JSONArray option = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();

                        if (option != null) {
                            for (int u = 0; u < option.size(); u++) {
                                JSONObject jsonobject = (JSONObject) option.get(u);
                                int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                int param = Integer.parseInt(jsonobject.get("param").toString());
                                optionList.add(new Item.ItemOption(optionId, param));
                            }
                        }
                        giftcode.option.put(id, optionList);
                        giftcode.detail.put(id, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            Logger.log("Downloaded Loading giftcode (" + GiftCodeManager.gI().listGiftCode.size() + ")\n");

        } catch (Exception ex) {

        }

        try (Connection con = DBConnecter.getConnectionServer();) {
            // load part
            ps = con.prepareStatement("select * from part");
            rs = ps.executeQuery();
            List<Part> parts = new ArrayList<>();
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getShort("id");
                part.type = rs.getByte("type");
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data").replaceAll("\\\"", ""));
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray pd = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    part.partDetails.add(new PartDetail(Short.parseShort(String.valueOf(pd.get(0))),
                            Byte.parseByte(String.valueOf(pd.get(1))),
                            Byte.parseByte(String.valueOf(pd.get(2)))));
                    pd.clear();
                }
                parts.add(part);
                dataArray.clear();
            }
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"));
            dos.writeShort(parts.size());
            for (Part part : parts) {
                dos.writeByte(part.type);
                for (PartDetail partDetail : part.partDetails) {
                    dos.writeShort(partDetail.iconId);
                    dos.writeByte(partDetail.dx);
                    dos.writeByte(partDetail.dy);
                }
            }
            dos.flush();
            Logger.log("Downloaded Loading part (" + parts.size() + ")\n");

            // load bg item template
            ps = con.prepareStatement("select * from bg_item_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BgItem bgItem = new BgItem();
                bgItem.id = rs.getInt("id");
                bgItem.layer = rs.getByte("layer");
                bgItem.dx = rs.getShort("dx");
                bgItem.dy = rs.getShort("dy");
                bgItem.idImage = rs.getShort("image_id");
                BG_ITEMS.add(bgItem);
            }
            Logger.log("Downloaded Loading bg item template (" + BG_ITEMS.size() + ")\n");

            // load array head 2 frames
            ps = con.prepareStatement("select * from array_head_2_frames");
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrHead2Frames arrHead2Frames = new ArrHead2Frames();
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                for (int i = 0; i < dataArray.size(); i++) {
                    arrHead2Frames.frames.add(Integer.valueOf(dataArray.get(i).toString()));
                }
                ARR_HEAD_2_FRAMES.add(arrHead2Frames);
            }
            Logger.log("Downloaded Loading arr head 2 frames (" + ARR_HEAD_2_FRAMES.size() + ")\n");

            // load skill
            ps = con.prepareStatement("select * from skill_template order by nclass_id, slot");
            rs = ps.executeQuery();
            byte nClassId = -1;
            NClass nClass = null;
            while (rs.next()) {
                byte id = rs.getByte("nclass_id");
                if (id != nClassId) {
                    nClassId = id;
                    nClass = new NClass();
                    nClass.name = id == ConstPlayer.TRAI_DAT ? "Trái Đất" : id == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
                    nClass.classId = nClassId;
                    NCLASS.add(nClass);
                }
                SkillTemplate skillTemplate = new SkillTemplate();
                skillTemplate.classId = nClassId;
                skillTemplate.id = rs.getByte("id");
                skillTemplate.name = rs.getString("name");
                skillTemplate.maxPoint = rs.getByte("max_point");
                skillTemplate.manaUseType = rs.getByte("mana_use_type");
                skillTemplate.type = rs.getByte("type");
                skillTemplate.iconId = rs.getShort("icon_id");
                skillTemplate.damInfo = rs.getString("dam_info");
                skillTemplate.description = rs.getString("description");
                nClass.skillTemplatess.add(skillTemplate);

                dataArray = (JSONArray) JSONValue.parse(
                        rs.getString("skills")
                                .replaceAll("\\[\"", "[")
                                .replaceAll("\"\\[", "[")
                                .replaceAll("\"\\]", "]")
                                .replaceAll("\\]\"", "]")
                                .replaceAll("\\}\",\"\\{", "},{"));
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    Skill skill = new Skill();
                    skill.template = skillTemplate;
                    skill.skillId = Short.parseShort(String.valueOf(dts.get("id")));
                    skill.point = Byte.parseByte(String.valueOf(dts.get("point")));
                    skill.powRequire = Long.parseLong(String.valueOf(dts.get("power_require")));
                    skill.manaUse = Integer.parseInt(String.valueOf(dts.get("mana_use")));
                    skill.coolDown = Integer.parseInt(String.valueOf(dts.get("cool_down")));
                    skill.dx = Integer.parseInt(String.valueOf(dts.get("dx")));
                    skill.dy = Integer.parseInt(String.valueOf(dts.get("dy")));
                    skill.maxFight = Integer.parseInt(String.valueOf(dts.get("max_fight")));
                    skill.damage = Short.parseShort(String.valueOf(dts.get("damage")));
                    skill.price = Short.parseShort(String.valueOf(dts.get("price")));
                    skill.moreInfo = String.valueOf(dts.get("info"));
                    skillTemplate.skillss.add(skill);
                }
            }
            Logger.log("Downloaded Loading skill (" + NCLASS.size() + ")\n");

            // load head avatar
            ps = con.prepareStatement("select * from head_avatar");
            rs = ps.executeQuery();
            while (rs.next()) {
                HeadAvatar headAvatar = new HeadAvatar(rs.getInt("head_id"), rs.getInt("avatar_id"));
                HEAD_AVATARS.add(headAvatar);
            }
            Logger.log("Downloaded Loading head avatar (" + HEAD_AVATARS.size() + ")\n");

            // load flag bag
            ps = con.prepareStatement("select * from flag_bag");
            rs = ps.executeQuery();
            while (rs.next()) {
                FlagBag flagBag = new FlagBag();
                flagBag.id = rs.getInt("id");
                flagBag.name = rs.getString("name");
                flagBag.gold = rs.getInt("gold");
                flagBag.gem = rs.getInt("gem");
                flagBag.iconId = rs.getShort("icon_id");
                String[] iconData = rs.getString("icon_data").split(",");
                flagBag.iconEffect = new short[iconData.length];
                for (int j = 0; j < iconData.length; j++) {
                    flagBag.iconEffect[j] = Short.parseShort(iconData[j].trim());
                }
                FLAGS_BAGS.add(flagBag);
            }
            Logger.log("Downloaded Loading flag bag (" + FLAGS_BAGS.size() + ")\n");

            // load intrinsic
            ps = con.prepareStatement("select * from intrinsic");
            rs = ps.executeQuery();
            while (rs.next()) {
                Intrinsic intrinsic = new Intrinsic();
                intrinsic.id = rs.getByte("id");
                intrinsic.name = rs.getString("name");
                intrinsic.paramFrom1 = rs.getShort("param_from_1");
                intrinsic.paramTo1 = rs.getShort("param_to_1");
                intrinsic.paramFrom2 = rs.getShort("param_from_2");
                intrinsic.paramTo2 = rs.getShort("param_to_2");
                intrinsic.icon = rs.getShort("icon");
                intrinsic.gender = rs.getByte("gender");
                switch (intrinsic.gender) {
                    case ConstPlayer.TRAI_DAT ->
                        INTRINSIC_TD.add(intrinsic);
                    case ConstPlayer.NAMEC ->
                        INTRINSIC_NM.add(intrinsic);
                    case ConstPlayer.XAYDA ->
                        INTRINSIC_XD.add(intrinsic);
                    default -> {
                        INTRINSIC_TD.add(intrinsic);
                        INTRINSIC_NM.add(intrinsic);
                        INTRINSIC_XD.add(intrinsic);
                    }
                }
                INTRINSICS.add(intrinsic);
            }
            Logger.log("Downloaded Loading intrinsic (" + INTRINSICS.size() + ")\n");

            // load task
            ps = con.prepareStatement("SELECT id, task_main_template.name, detail, "
                    + "task_sub_template.name AS 'sub_name', max_count, notify, npc_id, map "
                    + "FROM task_main_template JOIN task_sub_template ON task_main_template.id = "
                    + "task_sub_template.task_main_id");
            rs = ps.executeQuery();
            int taskId = -1;
            TaskMain task = null;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != taskId) {
                    taskId = id;
                    task = new TaskMain();
                    task.id = taskId;
                    task.name = rs.getString("name");
                    task.detail = rs.getString("detail");
                    TASKS.add(task);
                }
                SubTaskMain subTask = new SubTaskMain();
                subTask.name = rs.getString("sub_name");
                subTask.maxCount = rs.getShort("max_count");
                subTask.notify = rs.getString("notify");
                subTask.npcId = rs.getByte("npc_id");
                subTask.mapId = rs.getShort("map");
                task.subTasks.add(subTask);
            }
            Logger.log("Downloaded Loading task (" + TASKS.size() + ")\n");

            // load side task
            ps = con.prepareStatement("select * from side_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                SideTaskTemplate sideTask = new SideTaskTemplate();
                sideTask.id = rs.getInt("id");
                sideTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                sideTask.count[0][0] = Integer.parseInt(mc1[0]);
                sideTask.count[0][1] = Integer.parseInt(mc1[1]);
                sideTask.count[1][0] = Integer.parseInt(mc2[0]);
                sideTask.count[1][1] = Integer.parseInt(mc2[1]);
                sideTask.count[2][0] = Integer.parseInt(mc3[0]);
                sideTask.count[2][1] = Integer.parseInt(mc3[1]);
                sideTask.count[3][0] = Integer.parseInt(mc4[0]);
                sideTask.count[3][1] = Integer.parseInt(mc4[1]);
                sideTask.count[4][0] = Integer.parseInt(mc5[0]);
                sideTask.count[4][1] = Integer.parseInt(mc5[1]);
                SIDE_TASKS_TEMPLATE.add(sideTask);
            }
            Logger.log("Downloaded Loading side task (" + SIDE_TASKS_TEMPLATE.size() + ")\n");

            // load task badges
            ps = con.prepareStatement("select * from task_badges_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BadgesTaskTemplate badgesTaskTemplate = new BadgesTaskTemplate();
                badgesTaskTemplate.id = rs.getInt("id");
                badgesTaskTemplate.name = rs.getString("NAME");
                badgesTaskTemplate.count = rs.getInt("maxCount");
                badgesTaskTemplate.idbadgesReward = rs.getInt("idbadgesReward");
                TASKS_BADGES_TEMPLATE.add(badgesTaskTemplate);
            }
            Logger.log("Downloaded Loading task badges (" + TASKS_BADGES_TEMPLATE.size() + ")\n");

            // load clan task
            ps = con.prepareStatement("select * from clan_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ClanTaskTemplate clanTask = new ClanTaskTemplate();
                clanTask.id = rs.getInt("id");
                clanTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                clanTask.count[0][0] = Integer.parseInt(mc1[0]);
                clanTask.count[0][1] = Integer.parseInt(mc1[1]);
                clanTask.count[1][0] = Integer.parseInt(mc2[0]);
                clanTask.count[1][1] = Integer.parseInt(mc2[1]);
                clanTask.count[2][0] = Integer.parseInt(mc3[0]);
                clanTask.count[2][1] = Integer.parseInt(mc3[1]);
                clanTask.count[3][0] = Integer.parseInt(mc4[0]);
                clanTask.count[3][1] = Integer.parseInt(mc4[1]);
                clanTask.count[4][0] = Integer.parseInt(mc5[0]);
                clanTask.count[4][1] = Integer.parseInt(mc5[1]);
                CLAN_TASKS_TEMPLATE.add(clanTask);
            }
            Logger.log("Downloaded Loading clan task (" + CLAN_TASKS_TEMPLATE.size() + ")\n");

            // load achievement template
            ps = con.prepareStatement("select * from achievement_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate(rs.getString("info1"), rs.getString("info2"),
                        rs.getInt("money"), rs.getLong("max_count")));
            }
            Logger.log("Downloaded Loading achievement (" + ACHIEVEMENT_TEMPLATE.size() + ")\n");

            // load item template
            ps = con.prepareStatement("select * from item_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemTemplate itemTemp = new ItemTemplate();
                itemTemp.id = rs.getShort("id");
                itemTemp.type = rs.getByte("type");
                itemTemp.gender = rs.getByte("gender");
                itemTemp.name = rs.getString("name");
                itemTemp.description = rs.getString("description");
                itemTemp.level = rs.getByte("level");
                itemTemp.iconID = rs.getShort("icon_id");
                itemTemp.part = rs.getShort("part");
                itemTemp.isUpToUp = rs.getBoolean("is_up_to_up");
                itemTemp.strRequire = rs.getInt("power_require");
                itemTemp.gold = rs.getInt("gold");
                itemTemp.gem = rs.getInt("gem");
                itemTemp.head = rs.getInt("head");
                itemTemp.body = rs.getInt("body");
                itemTemp.leg = rs.getInt("leg");
                ITEM_TEMPLATES.add(itemTemp);
            }
            Logger.log("Downloaded Loading map item template (" + ITEM_TEMPLATES.size() + ")\n");

            // load item option template
            ps = con.prepareStatement("select id, name from item_option_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemOptionTemplate optionTemp = new ItemOptionTemplate();
                optionTemp.id = rs.getInt("id");
                optionTemp.name = rs.getString("name");
                ITEM_OPTION_TEMPLATES.add(optionTemp);
            }
            Logger.log("Downloaded Loading map item option template (" + ITEM_OPTION_TEMPLATES.size() + ")\n");

            // load kol task
            ps = con.prepareStatement("select * from task_kol_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                KOL_TASKS_TEMPLATE
                        .add(new KolTaskTemplate(rs.getInt("id"), rs.getString("info"), rs.getInt("max_count")));
            }
            Logger.log("Downloaded Loading KOL task (" + KOL_TASKS_TEMPLATE.size() + ")\n");

            // load shop
            SHOPS = ShopDAO.getShops(con);
            Logger.log("Downloaded Loading shop (" + SHOPS.size() + ")\n");

            // load notify
            ps = con.prepareStatement("select * from notify order by id desc");
            rs = ps.executeQuery();
            while (rs.next()) {
                NOTIFY.add(rs.getString("name") + "<>" + rs.getString("text"));
            }
            Logger.log("Downloaded Loading notify (" + NOTIFY.size() + ")\n");

            // load image by name
            ps = con.prepareStatement("select name, n_frame from img_by_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                IMAGES_BY_NAME.put(rs.getString("name"), rs.getByte("n_frame"));
            }
            Logger.log("Downloaded Loading images by name (" + IMAGES_BY_NAME.size() + ")\n");

            // Load mount
            for (ItemTemplate item : ITEM_TEMPLATES) {
                if (item.type == 23 && getNFrameImageByName("mount_" + item.part + "_0") != 0) {
                    MAP_MOUNT_NUM.put(item.id, (short) (item.part + 30000));
                }
            }
            Logger.log("Downloaded Loading mount (" + MAP_MOUNT_NUM.size() + ")\n");

            PowerLimitManager.getInstance().load();
            CaptionManager.getInstance().load();

            // Load item ký gửi
            ps = con.prepareStatement("SELECT * FROM shop_ky_gui");
            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    int i = rs.getInt("id");
                    int idPl = rs.getInt("player_id");
                    byte tab = rs.getByte("tab");
                    short itemId = rs.getShort("item_id");
                    int gold = rs.getInt("gold");
                    int gem = rs.getInt("gem");
                    int quantity = rs.getInt("quantity");
                    byte isUp = rs.getByte("isUpTop");
                    boolean isBuy = rs.getByte("isBuy") == 1;

                    List<Item.ItemOption> op = new ArrayList<>();

                    String itemOptionStr = rs.getString("itemOption");
                    if (itemOptionStr != null && !itemOptionStr.trim().isEmpty()) {
                        Object parsed = JSONValue.parse(itemOptionStr);

                        if (parsed instanceof JSONArray jsa2) {
                            for (Object obj : jsa2) {
                                if (obj instanceof JSONObject jso2) {
                                    Object idOpt = jso2.get("id");
                                    Object paramOpt = jso2.get("param");
                                    if (idOpt != null && paramOpt != null) {
                                        int idOptions = Integer.parseInt(idOpt.toString());
                                        int param = Integer.parseInt(paramOpt.toString());
                                        op.add(new Item.ItemOption(idOptions, param));
                                    }
                                } else if (obj instanceof JSONArray arr && arr.size() >= 2) {
                                    int idOptions = Integer.parseInt(arr.get(0).toString());
                                    int param = Integer.parseInt(arr.get(1).toString());
                                    op.add(new Item.ItemOption(idOptions, param));
                                }
                            }
                        } else if (parsed instanceof JSONObject jsonObj) {
                            Object optObj = jsonObj.get("options");
                            if (optObj instanceof JSONArray arrOpts) {
                                for (Object o : arrOpts) {
                                    if (o instanceof JSONArray arr && arr.size() >= 2) {
                                        int idOptions = Integer.parseInt(arr.get(0).toString());
                                        int param = Integer.parseInt(arr.get(1).toString());
                                        op.add(new Item.ItemOption(idOptions, param));
                                    } else if (o instanceof JSONObject jso2) {
                                        Object idOpt = jso2.get("id");
                                        Object paramOpt = jso2.get("param");
                                        if (idOpt != null && paramOpt != null) {
                                            int idOptions = Integer.parseInt(idOpt.toString());
                                            int param = Integer.parseInt(paramOpt.toString());
                                            op.add(new Item.ItemOption(idOptions, param));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ConsignShopManager.gI().listItem.add(
                            new ConsignItem(i, itemId, idPl, tab, gold, gem, quantity, isUp, op, isBuy));

                } catch (Exception e) {
                    System.err.println("Lỗi khi load item ký gửi ID=" + rs.getInt("id") + ": " + e.getMessage());
                }
            }
            Logger.log("Downloaded Loading Consign Item (" + ConsignShopManager.gI().listItem.size() + ")\n");

            // load mob template
            ps = con.prepareStatement("select * from mob_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                MobTemplate mobTemp = new MobTemplate();
                mobTemp.id = rs.getByte("id");
                mobTemp.type = rs.getByte("type");
                mobTemp.name = rs.getString("name");
                mobTemp.hp = rs.getInt("hp");
                mobTemp.rangeMove = rs.getByte("range_move");
                mobTemp.speed = rs.getByte("speed");
                mobTemp.dartType = rs.getByte("dart_type");
                mobTemp.percentDame = rs.getByte("percent_dame");
                mobTemp.percentTiemNang = rs.getByte("percent_tiem_nang");
                MOB_TEMPLATES.add(mobTemp);
            }
            Logger.log("Downloaded Loading mob template (" + MOB_TEMPLATES.size() + ")\n");

            // load npc template
            ps = con.prepareStatement("select * from npc_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                NPC_TEMPLATES.add(npcTemp);
            }
            Logger.log("Downloaded Loading npc template (" + NPC_TEMPLATES.size() + ")\n");

            // load map template
            ps = con.prepareStatement("select count(id) from map_template");
            rs = ps.executeQuery();
            if (rs.next()) {
                int countRow = rs.getShort(1);
                MAP_TEMPLATES = new MapTemplate[countRow];
                ps = con.prepareStatement("select * from map_template");
                rs = ps.executeQuery();
                short i = 0;
                while (rs.next()) {
                    MapTemplate mapTemplate = new MapTemplate();
                    int mapId = rs.getInt("id");
                    String mapName = rs.getString("name");
                    mapTemplate.id = mapId;
                    mapTemplate.name = mapName;
                    mapTemplate.type = rs.getByte("type");
                    mapTemplate.planetId = rs.getByte("planet_id");
                    mapTemplate.bgType = rs.getByte("bg_type");
                    mapTemplate.tileId = rs.getByte("tile_id");
                    mapTemplate.bgId = rs.getByte("bg_id");
                    mapTemplate.zones = rs.getByte("zones");
                    mapTemplate.maxPlayerPerZone = rs.getByte("max_player");
                    // load waypoints
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                            .replaceAll("\\[\"\\[", "[[")
                            .replaceAll("\\]\"\\]", "]]")
                            .replaceAll("\",\"", ","));
                    for (int j = 0; j < dataArray.size(); j++) {
                        WayPoint wp = new WayPoint();
                        JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        wp.name = String.valueOf(dtwp.get(0));
                        wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                        wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                        wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                        wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                        wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                        wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                        wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                        wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                        wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                        mapTemplate.wayPoints.add(wp);
                        dtwp.clear();
                    }
                    dataArray.clear();
                    // load mobs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                    mapTemplate.mobTemp = new byte[dataArray.size()];
                    mapTemplate.mobLevel = new byte[dataArray.size()];
                    mapTemplate.mobHp = new int[dataArray.size()];
                    mapTemplate.mobX = new short[dataArray.size()];
                    mapTemplate.mobY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.mobTemp[j] = Byte.parseByte(String.valueOf(dtm.get(0)));
                        mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                        mapTemplate.mobHp[j] = (int) Long.parseLong(String.valueOf(dtm.get(2)));
                        mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                        mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                        dtm.clear();
                    }
                    dataArray.clear();
                    // load npcs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                    mapTemplate.npcId = new byte[dataArray.size()];
                    mapTemplate.npcX = new short[dataArray.size()];
                    mapTemplate.npcY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                        mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                        mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                        dtn.clear();
                    }
                    dataArray.clear();
                    // load eff sữa effect
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("effect"));
                    // for (int j = 0; j < dataArray.size(); j++) {
                    // EffectMap em = new EffectMap();
                    // dataObject = (JSONObject) JSONValue.parse(dataArray.get(j).toString());
                    // em.setKey(String.valueOf(dataObject.get("key")));
                    // em.setValue(String.valueOf(dataObject.get("value")));
                    // mapTemplate.effectMaps.add(em);
                    // }
                    EffectMap em = new EffectMap();
                    em.setKey("beff");
                    em.setValue("15");
                    mapTemplate.effectMaps.add(em);

                    dataArray.clear();
                    MAP_TEMPLATES[i++] = mapTemplate;
                }
                Logger.log("Downloaded Loading map template (" + MAP_TEMPLATES.length + ")\n");
            }
            EffectEventManager.gI().load();
            ps = con.prepareStatement("select * from radar");
            rs = ps.executeQuery();
            while (rs.next()) {
                RadarCard rd = new RadarCard();
                rd.Id = rs.getShort("id");
                rd.IconId = rs.getShort("iconId");
                rd.Rank = rs.getByte("rank");
                rd.Max = rs.getByte("max");
                rd.Type = rs.getByte("type");
                rd.Template = rs.getShort("mob_id");
                rd.Name = rs.getString("name");
                rd.Info = rs.getString("info");
                JSONArray arr = (JSONArray) JSONValue.parse(rs.getString("body"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Head = Short.parseShort(ob.get("head").toString());
                        rd.Body = Short.parseShort(ob.get("body").toString());
                        rd.Leg = Short.parseShort(ob.get("leg").toString());
                        rd.Bag = Short.parseShort(ob.get("bag").toString());
                    }
                }
                rd.Options.clear();
                arr = (JSONArray) JSONValue.parse(rs.getString("options"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Options.add(new OptionCard(Integer.parseInt(ob.get("id").toString()),
                                Short.parseShort(ob.get("param").toString()),
                                Byte.parseByte(ob.get("activeCard").toString())));
                    }
                }
                rd.AuraId = rs.getShort("aura_id");
                RadarService.gI().RADAR_TEMPLATE.add(rd);
            }
            Logger.log("Downloaded Loading radar template (" + RadarService.gI().RADAR_TEMPLATE.size() + ")\n");

            File directory = new File("data/icon/x4");
            if (directory.isDirectory()) {
                Optional<File> maxFile = Arrays.stream(directory.listFiles())
                        .filter(File::isFile)
                        .filter(file -> file.getName().endsWith(".png"))
                        .max(Comparator.comparingInt(file -> {
                            String name = file.getName();
                            return Integer.valueOf(name.substring(0, name.length() - 4));
                        }));
                if (maxFile.isPresent()) {
                    String fileName = maxFile.get().getName();
                    short maxVersion = Short.parseShort(fileName.substring(0, fileName.length() - 4));
                    DataGame.maxSmallVersion = (short) (maxVersion + 1);
                    Logger.log("Downloaded Loading max small version (" + DataGame.maxSmallVersion + ")\n");
                }
            }

            ps = con.prepareStatement("select * from data_badges");
            rs = ps.executeQuery();
            while (rs.next()) {
                BagesTemplate template = new BagesTemplate();
                template.id = rs.getInt("id");
                template.idEffect = rs.getInt("idEffect");
                template.idItem = rs.getInt("idItem");
                template.NAME = rs.getString("NAME");

                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("Options"));
                ;
                if (option != null) {
                    for (int u = 0; u < option.size(); u++) {
                        JSONObject jsonobject = (JSONObject) option.get(u);
                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                        int param = Integer.parseInt(jsonobject.get("param").toString());
                        template.options.add(new Item.ItemOption(optionId, param));
                    }
                }
                BAGES_TEMPLATES.add(template);
            }
            Logger.log("Loaded badge templates (" + BAGES_TEMPLATES.size() + ")\n");

            topNV = realTop(ConstSQL.TOP_NV, con);
            Logger.log("Loaded Task Ranking (" + topNV.size() + ")\n");

            topSM = realTop(ConstSQL.TOP_SM, con);
            Logger.log("Loaded Power Ranking (" + topSM.size() + ")\n");

            topNap = realTop(ConstSQL.TOP_NAP, con);
            Logger.log("Loaded Top Recharge Ranking (" + topNap.size() + ")\n");

            topWHIS = realTop(ConstSQL.TOP_WHIS, con);
            Logger.log("Loaded WHIS Ranking (" + topWHIS.size() + ")\n");

            topVDST = realTop(ConstSQL.TOP_VDST, con);
            Logger.log("Loaded VDST Ranking (" + topVDST.size() + ")\n");

            topDuaSM = realTop(ConstSQL.TOP_DUA_SM, con);
            Logger.log("Loaded Power Race Ranking (" + topDuaSM.size() + ")\n");

            topDuaNap = realTop(ConstSQL.TOP_DUA_NAP, con);
            Logger.log("Loaded Recharge Race Ranking (" + topDuaNap.size() + ")\n");

            Topmaydam = realTop(ConstSQL.queryTopmaydam, con);
            Logger.log("Loaded Punching Machine Power Ranking (" + Topmaydam.size() + ")\n");

            Manager.timeRealTop = System.currentTimeMillis();
        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Error while loading rankings from database");

            System.exit(0);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
        Logger.log("Total database loading time: " + (System.currentTimeMillis() - st) + " (ms)\n");
    }

    public void updateShop() {
        try (Connection con = DBConnecter.getConnectionServer();) {
            SHOPS = ShopDAO.getShops(con);
        } catch (Exception ex) {

        }
    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

            int index = 0; // dùng riêng cho TOP_WHIS và TOP_VDST
            while (rs.next()) {
                // mặc định head/body/leg theo giới tính
                byte gender = rs.getByte("gender");
                short head = Util.getHead(gender);
                short body = (short) (gender == 1 ? 59 : 57);
                short leg = (short) (gender == 1 ? 60 : 58);

                // đọc items_body
                String itemsBodyJson = rs.getString("items_body");
                if (itemsBodyJson != null) {
                    JSONArray dataArray = (JSONArray) JSONValue.parse(itemsBodyJson);
                    if (dataArray != null) {
                        // slot 0 â†’ body
                        body = getItemPart(dataArray, 0, body, "body");

                        // slot 1 â†’ leg
                        leg = getItemPart(dataArray, 1, leg, "leg");

                        // slot 5 â†’ head/body/leg override
                        short[] parts = getItemParts(dataArray, 5, head, body, leg);
                        head = parts[0];
                        body = parts[1];
                        leg = parts[2];
                    }
                }

                // build object TOP
                TOP top = TOP.builder()
                        .name(rs.getString("name"))
                        .gender(gender)
                        .head(head)
                        .body(body)
                        .leg(leg)
                        .build();

                // set dữ liệu riêng theo từng top
                switch (query) {
                    case ConstSQL.TOP_NV -> {
                        top.setNv(rs.getByte("nv"));
                        top.setSubnv(rs.getByte("subnv"));
                        top.setLasttime(rs.getLong("lasttime"));
                    }
                    case ConstSQL.TOP_DC -> {
                        top.setDicanh(rs.getInt("dicanh"));
                        top.setJuventus(rs.getInt("juventus"));
                    }
                    case ConstSQL.TOP_SM, "TOP_DUA_SM" -> {
                        top.setPower(rs.getBigDecimal("sm").longValue());
                    }
                    case ConstSQL.TOP_NAP -> {
                        top.setCash(rs.getInt("cash"));
                    }
                    case ConstSQL.TOP_DUA_NAP -> {
                        top.setCash(rs.getInt("danap"));
                    }
                    case ConstSQL.TOP_DUA_QUOC_VUONG -> {
                        top.setThoivang(rs.getInt("thoi_vang"));
                    }
                    case ConstSQL.TOP_WHIS -> {
                        top.setLasttime(rs.getLong("lasttime"));
                        top.setLevel(rs.getInt("top"));
                        top.setTime(rs.getInt("time"));
                        index++;
                    }
                    case ConstSQL.TOP_VDST -> {
                        top.setDivdst(rs.getInt("time"));
                        top.setLasttime(rs.getLong("lasttime"));
                        index++;
                    }
                    case ConstSQL.queryTopmaydam -> {
                        int maydam = rs.getInt("point_maydam");
                        long totalDame = rs.getBigDecimal("total_damage_maydam").longValue();
                        top.setId_player(rs.getInt("id")); // ⚡ quan trọng
                        top.setInfo1(maydam + " điểm");
                        top.setInfo2(Util.formatNumber(totalDame) + " sát thương");
                        index++;
                    }
                }

                tops.add(top);
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc realTop: " + e.getMessage());
            e.printStackTrace();
        }

        return tops;
    }

    /**
     * Lấy part item từ dataArray (slot body hoặc leg).
     */
    private static short getItemPart(JSONArray dataArray, int index, short defaultPart, String type) {
        try {
            JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(index).toString());
            if (dataItem != null && dataItem.get(0) != null) {
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1) {
                    Item item = ItemService.gI().createNewItem(
                            tempId,
                            Integer.parseInt(String.valueOf(dataItem.get(1))));
                    return (short) item.template.part;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultPart;
    }

    /**
     * Lấy head/body/leg override từ item slot 5.
     */
    private static short[] getItemParts(JSONArray dataArray, int index, short head, short body, short leg) {
        try {
            JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(index).toString());
            if (dataItem != null && dataItem.get(0) != null) {
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1) {
                    Item item = ItemService.gI().createNewItem(
                            tempId,
                            Integer.parseInt(String.valueOf(dataItem.get(1))));
                    if (item.template.head != -1) {
                        head = (short) item.template.head;
                    }
                    if (item.template.body != -1) {
                        body = (short) item.template.body;
                    }
                    if (item.template.leg != -1) {
                        leg = (short) item.template.leg;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new short[] { head, body, leg };
    }

    public void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("data/config/config.properties"));
        Object value;
        if ((value = properties.get("server.sv")) != null) {
            SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.name")) != null) {
            String name = String.valueOf(value);
            ServerManager.NAME = name;
        }
        if ((value = properties.get("server.port_real")) != null) {
            AntiDDoS_BY_Barcoll.REAL_PORT = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.port_proxy")) != null) {
            AntiDDoS_BY_Barcoll.PROXY_PORT = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.ip_host")) != null) {
            AntiDDoS_BY_Barcoll.REAL_HOST = String.valueOf(value);
        }
        String linkServer = "";
        if ((value = properties.get("server.ip")) != null) {
            AntiDDoS_BY_Barcoll.REAL_HOST = String.valueOf(value);
            linkServer += ServerManager.NAME + ":" + AntiDDoS_BY_Barcoll.REAL_HOST + ":" + AntiDDoS_BY_Barcoll.REAL_PORT
                    + ":0,";
        }
        for (int i = 1; i <= 10; i++) {
            value = properties.get("server.sv" + i);
            if (value != null) {
                linkServer += String.valueOf(value) + ":0,";
            }
        }
        DataGame.LINK_IP_PORT = linkServer.substring(0, linkServer.length() - 1);
        if ((value = properties.get("server.waitlogin")) != null) {
            SECOND_WAIT_LOGIN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.maxperip")) != null) {
            MAX_PER_IP = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.maxplayer")) != null) {
            MAX_PLAYER = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.expserver")) != null) {
            RATE_EXP_SERVER = Double.parseDouble(String.valueOf(value));
        }
        if ((value = properties.get("server.local")) != null) {
            LOCAL = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.test")) != null) {
            TEST = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.daoautoupdater")) != null) {
            DAO_AUTO_UPDATER = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("auto.maintenance")) != null) {
            AUTO_MAINTENANCE = Integer.parseInt(String.valueOf(value).trim());
        }
        if ((value = properties.get("auto.maintenance.hour")) != null) {
            AUTO_MAINTENANCE_HOUR = Integer.parseInt(String.valueOf(value).trim());
        }
        if ((value = properties.get("auto.maintenance.minute")) != null) {
            AUTO_MAINTENANCE_MINUTE = Integer.parseInt(String.valueOf(value).trim());
        }
        if ((value = properties.get("server.event")) != null) {
            String[] eventIds = String.valueOf(value).split(",");
            for (String id : eventIds) {
                try {
                    int eventId = Integer.parseInt(id.trim());
                    Manager.ACTIVE_EVENTS.add(eventId);
                } catch (NumberFormatException e) {
                    System.err.println("[EventManager] Lỗi định dạng event id: " + id);
                }
            }
        }
        // Config Time Event SM
        if ((value = properties.get("event.sm.month_open")) != null) {
            ConstDataEventSM.MONTH_OPEN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.date_open")) != null) {
            ConstDataEventSM.DATE_OPEN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.hour_open")) != null) {
            ConstDataEventSM.HOUR_OPEN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.minute_open")) != null) {
            ConstDataEventSM.MIN_OPEN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.month_end")) != null) {
            ConstDataEventSM.MONTH_END = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.date_end")) != null) {
            ConstDataEventSM.DATE_END = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.hour_end")) != null) {
            ConstDataEventSM.HOUR_END = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.sm.minute_end")) != null) {
            ConstDataEventSM.MIN_END = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("event.year")) != null) {
            ConstDataEventSM.YEAR_EVENT = Short.parseShort(String.valueOf(value));
        }
        // Config Time Event TOP NAP
        if ((value = properties.get("event.nap.month_open")) != null) {
            ConstDataEventNAP.MONTH_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.date_open")) != null) {
            ConstDataEventNAP.DATE_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.hour_open")) != null) {
            ConstDataEventNAP.HOUR_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.minute_open")) != null) {
            ConstDataEventNAP.MIN_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.month_end")) != null) {
            ConstDataEventNAP.MONTH_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.date_end")) != null) {
            ConstDataEventNAP.DATE_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.hour_end")) != null) {
            ConstDataEventNAP.HOUR_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.nap.minute_end")) != null) {
            ConstDataEventNAP.MIN_END = Short.parseShort(String.valueOf(value));
        }
        // Config Time Event THIỆP CHÚC VIP
        if ((value = properties.get("event.chucvip.month_open")) != null) {
            ConstDataEventCHUCVIP.MONTH_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.date_open")) != null) {
            ConstDataEventCHUCVIP.DATE_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.hour_open")) != null) {
            ConstDataEventCHUCVIP.HOUR_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.minute_open")) != null) {
            ConstDataEventCHUCVIP.MIN_OPEN = Short.parseShort(String.valueOf(value));
        }

        if ((value = properties.get("event.chucvip.month_end")) != null) {
            ConstDataEventCHUCVIP.MONTH_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.date_end")) != null) {
            ConstDataEventCHUCVIP.DATE_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.hour_end")) != null) {
            ConstDataEventCHUCVIP.HOUR_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.chucvip.minute_end")) != null) {
            ConstDataEventCHUCVIP.MIN_END = Short.parseShort(String.valueOf(value));
        }
        // Config Time Event TRANG SỨC VIP
        if ((value = properties.get("event.trangsucvip.month_open")) != null) {
            ConstDataEventTRANGSUCVIP.MONTH_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.date_open")) != null) {
            ConstDataEventTRANGSUCVIP.DATE_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.hour_open")) != null) {
            ConstDataEventTRANGSUCVIP.HOUR_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.minute_open")) != null) {
            ConstDataEventTRANGSUCVIP.MIN_OPEN = Short.parseShort(String.valueOf(value));
        }

        if ((value = properties.get("event.trangsucvip.month_end")) != null) {
            ConstDataEventTRANGSUCVIP.MONTH_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.date_end")) != null) {
            ConstDataEventTRANGSUCVIP.DATE_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.hour_end")) != null) {
            ConstDataEventTRANGSUCVIP.HOUR_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.trangsucvip.minute_end")) != null) {
            ConstDataEventTRANGSUCVIP.MIN_END = Short.parseShort(String.valueOf(value));
        }
        // Config Time Event 20/10 (THÁNG MƯỜI)
        if ((value = properties.get("event.thangmuoi.month_open")) != null) {
            ConstDataEventthangmuoi.MONTH_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.date_open")) != null) {
            ConstDataEventthangmuoi.DATE_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.hour_open")) != null) {
            ConstDataEventthangmuoi.HOUR_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.minute_open")) != null) {
            ConstDataEventthangmuoi.MIN_OPEN = Short.parseShort(String.valueOf(value));
        }

        if ((value = properties.get("event.thangmuoi.month_end")) != null) {
            ConstDataEventthangmuoi.MONTH_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.date_end")) != null) {
            ConstDataEventthangmuoi.DATE_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.hour_end")) != null) {
            ConstDataEventthangmuoi.HOUR_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.thangmuoi.minute_end")) != null) {
            ConstDataEventthangmuoi.MIN_END = Short.parseShort(String.valueOf(value));
        }
        // Config Time Event ĐUA TOP
        if ((value = properties.get("event.top.month_open")) != null) {
            ConstDataEventTOP.MONTH_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.date_open")) != null) {
            ConstDataEventTOP.DATE_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.hour_open")) != null) {
            ConstDataEventTOP.HOUR_OPEN = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.minute_open")) != null) {
            ConstDataEventTOP.MIN_OPEN = Short.parseShort(String.valueOf(value));
        }

        if ((value = properties.get("event.top.month_end")) != null) {
            ConstDataEventTOP.MONTH_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.date_end")) != null) {
            ConstDataEventTOP.DATE_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.hour_end")) != null) {
            ConstDataEventTOP.HOUR_END = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.minute_end")) != null) {
            ConstDataEventTOP.MIN_END = Short.parseShort(String.valueOf(value));
        }

        if ((value = properties.get("event.top.month_reward")) != null) {
            ConstDataEventTOP.MONTH_REWARD = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.date_reward")) != null) {
            ConstDataEventTOP.DATE_REWARD = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.hour_reward")) != null) {
            ConstDataEventTOP.HOUR_REWARD = Short.parseShort(String.valueOf(value));
        }
        if ((value = properties.get("event.top.minute_reward")) != null) {
            ConstDataEventTOP.MIN_REWARD = Short.parseShort(String.valueOf(value));
        }
        ConstDataEventSM.initsukien = false;
        ConstDataEventSM.isTraoQua = true;
        ConstDataEventNAP.initsukien = false;
        ConstDataEventNAP.isTraoQua = true;

        // --- Load cấu hình rơi đồ Boss từ Panel ---
        Properties bossProps = new Properties();
        File bossFile = new File("data/config/boss_reward.properties");
        if (bossFile.exists()) {
            try (FileInputStream fis = new FileInputStream(bossFile)) {
                bossProps.load(fis);
                for (String key : bossProps.stringPropertyNames()) {
                    try {
                        int bossId = Integer.parseInt(key);
                        String items = bossProps.getProperty(key);
                        BOSS_REWARD_PANEL.put(bossId, items);
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi định dạng BossID trong boss_reward.properties: " + key);
                    }
                }
                System.out.println(">> Loaded Custom Boss Rewards: " + BOSS_REWARD_PANEL.size());
            } catch (IOException e) {
                System.err.println("Lỗi khi đọc file boss_reward.properties");
            }
        }
    }

    public static void saveBossRewardConfig() {
        try {
            Properties properties = new Properties();
            // Đưa toàn bộ dữ liệu từ Map vào Properties object
            for (Map.Entry<Integer, String> entry : BOSS_REWARD_PANEL.entrySet()) {
                properties.setProperty(String.valueOf(entry.getKey()), entry.getValue());
            }

            // Tạo thư mục nếu chưa có
            File folder = new File("data/config");
            if (!folder.exists())
                folder.mkdirs();

            // Lưu xuống file riêng biệt để dễ quản lý
            String path = "data/config/boss_reward.properties";
            try (FileOutputStream out = new FileOutputStream(path)) {
                properties.store(out, "Boss Reward Config Updated via Admin Panel");
            }
            System.out.println(">> Đã lưu file boss_reward.properties thành công!");
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi file boss_reward.properties: " + e.getMessage());
        }
    }

    public static final List<Integer> ACTIVE_EVENTS = new ArrayList<>();

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    private int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;

                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    /**
     * @param mapId mapId
     * @return tile map for paint
     */
    private int[][] readTileMap(int mapId) {
        int[][] tileMap = null;
        try {
            try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + mapId))) {
                int w = dis.readByte();
                int h = dis.readByte();
                tileMap = new int[h][w];
                for (int[] tm : tileMap) {
                    for (int j = 0; j < tm.length; j++) {
                        tm[j] = dis.readByte();
                    }
                }
            }
        } catch (IOException e) {
        }
        return tileMap;
    }

    public static Clan getClanById(int id) throws Exception {
        for (Clan clan : CLANS) {
            if (clan.id == id) {
                return clan;
            }
        }
        throw new Exception("Không tìm thấy clan id: " + id);
    }

    public static void addClan(Clan clan) {
        CLANS.add(clan);
    }

    public static int getNumClan() {
        return CLANS.size();

    }

    public static MobTemplate getMobTemplateByTemp(int mobTempId) {
        for (MobTemplate mobTemp : MOB_TEMPLATES) {
            if (mobTemp.id == mobTempId) {
                return mobTemp;
            }
        }
        return null;
    }

    public static CaiTrang getCaiTrangByItemId(int itemId) {
        for (CaiTrang caiTrang : CAI_TRANGS) {
            if (caiTrang.tempId == itemId) {
                return caiTrang;
            }
        }
        return null;
    }

    public static byte getNFrameImageByName(String name) {
        Object n = IMAGES_BY_NAME.get(name);
        if (n != null) {
            return Byte.parseByte(String.valueOf(n));
        } else {
            return 0;
        }
    }

    // Xử lý menu Top
    public static Timestamp timeSuKienDuaTop = Timestamp.valueOf("2025-09-21 23:59:59");
    public static String timeStartDuaTop = "10h ngày 25/5/2025";
    public static String timeEndDuaTop = "23h59 ngày 10/6/2025";
    public static String timeEndNhanGiai = "20h20 ngày 24/11/2025";

    public static String demTimeSuKien() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTop.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaTopTrungThuVip = Timestamp.valueOf("2025-10-10 23:59:59");

    public static String demTimeSuKienTrungThuVip() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTopTrungThuVip.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaTopmaquy = Timestamp.valueOf("2025-10-10 23:59:59");

    public static String demTimeSuKienmaquy() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTopmaquy.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaToptrahoacuc = Timestamp.valueOf("2025-10-10 23:59:59");

    public static String demTimeSuKientrahoacuc() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaToptrahoacuc.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaTopTrungThuVipNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");

    public static String demTimeSuKienTrungThuVipNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTopTrungThuVipNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaTopmaquyNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");

    public static String demTimeSuKienmaquyNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTopmaquyNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaToptrahoacucNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");

    public static String demTimeSuKientrahoacucNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaToptrahoacucNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDualongdentreo = Timestamp.valueOf("2025-10-25 23:59:59");

    public static String demTimeSuKienlongdentreo() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDualongdentreo.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }
    // ==============================
    // 🔸 Đếm thời gian còn lại sự kiện Trang Sức VIP
    // ==============================

    public static String demTimeSuKiencapsuvip() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = LocalDateTime.of(
                ConstDataEventSM.YEAR_EVENT,
                ConstDataEventTRANGSUCVIP.MONTH_END,
                ConstDataEventTRANGSUCVIP.DATE_END,
                ConstDataEventTRANGSUCVIP.HOUR_END,
                ConstDataEventTRANGSUCVIP.MIN_END);

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    // ==============================
    // 🔸 Đếm thời gian còn lại sự kiện Thiệp Chúc VIP
    // ==============================
    public static String demTimeSuKienthiepchucvip() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = LocalDateTime.of(
                ConstDataEventSM.YEAR_EVENT,
                ConstDataEventCHUCVIP.MONTH_END,
                ConstDataEventCHUCVIP.DATE_END,
                ConstDataEventCHUCVIP.HOUR_END,
                ConstDataEventCHUCVIP.MIN_END);

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    // ==============================
    // 🔸 Đếm thời gian còn lại sự kiện 20/10
    // ==============================
    public static String demTimeSuKien2010() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = LocalDateTime.of(
                ConstDataEventSM.YEAR_EVENT,
                ConstDataEventthangmuoi.MONTH_END,
                ConstDataEventthangmuoi.DATE_END,
                ConstDataEventthangmuoi.HOUR_END,
                ConstDataEventthangmuoi.MIN_END);

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienDuaToplongdentreoNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKienlongdentreoNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaToplongdentreoNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKiencapsuvipNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKiencapsuvipNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKiencapsuvipNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKienthiepvipNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKienthiepvipNhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienthiepvipNhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static Timestamp timeSuKien2010NhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKien2010NhanGiai() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKien2010NhanGiai.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    // 🎃 Event Halloween - Hộp Kẹo Ma Quỷ
    public static Timestamp timeSuKienKeoMaQuyEnd = Timestamp.valueOf("2025-11-10 23:59:59");
    public static Timestamp timeSuKienKeoMaQuyNhanGiai = Timestamp.valueOf("2025-11-12 23:59:59");

    // 👻 Event Halloween - Thiệp Halloween
    public static Timestamp timeSuKienThiepHalloweenEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienThiepHalloweenNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");

    // 🎭 Event Halloween - Túi Mù Halloween
    public static Timestamp timeSuKienTuiMuHalloweenEnd = Timestamp.valueOf("2025-11-05 23:59:59");
    public static Timestamp timeSuKienTuiMuHalloweenNhanGiai = Timestamp.valueOf("2025-11-07 23:59:59");

    // ======== HỘP KẸO MA QUỶ ========
    public static String demTimeKeoMaQuy() {
        return demConLai(timeSuKienKeoMaQuyEnd);
    }

    public static String demTimeKeoMaQuyNhanGiai() {
        return demConLai(timeSuKienKeoMaQuyNhanGiai);
    }

    // ======== THIỆP HALLOWEEN ========
    public static String demTimeThiepHalloween() {
        return demConLai(timeSuKienThiepHalloweenEnd);
    }

    public static String demTimeThiepHalloweenNhanGiai() {
        return demConLai(timeSuKienThiepHalloweenNhanGiai);
    }

    // ======== TÃšI MÃ™ HALLOWEEN ========
    public static String demTimeTuiMuHalloween() {
        return demConLai(timeSuKienTuiMuHalloweenEnd);
    }

    public static String demTimeTuiMuHalloweenNhanGiai() {
        return demConLai(timeSuKienTuiMuHalloweenNhanGiai);
    }

    // ======== Hàm dùng chung để tính số ngày còn lại ========
    private static String demConLai(Timestamp eventTimeStamp) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = eventTimeStamp.toLocalDateTime();
        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static long demTimeSuKien2() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTop.toLocalDateTime();

        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return daysRemaining;
        } else {
            return 0;
        }
    }

    // End xử lý menu top
    public static void saveProperties() {
        try {
            Properties properties = new Properties();
            String path = "data/config/config.properties";

            // Đọc file hiện tại
            try (FileInputStream in = new FileInputStream(path)) {
                properties.load(in);
            }

            // 1. Lưu Năm chung
            properties.setProperty("event.year", String.valueOf(ConstDataEventSM.YEAR_EVENT));

            // 2. Lưu các mốc Đua Top
            properties.setProperty("event.top.month_open", String.valueOf(ConstDataEventTOP.MONTH_OPEN));
            properties.setProperty("event.top.date_open", String.valueOf(ConstDataEventTOP.DATE_OPEN));
            properties.setProperty("event.top.hour_open", String.valueOf(ConstDataEventTOP.HOUR_OPEN));
            properties.setProperty("event.top.minute_open", String.valueOf(ConstDataEventTOP.MIN_OPEN));

            properties.setProperty("event.top.month_end", String.valueOf(ConstDataEventTOP.MONTH_END));
            properties.setProperty("event.top.date_end", String.valueOf(ConstDataEventTOP.DATE_END));
            properties.setProperty("event.top.hour_end", String.valueOf(ConstDataEventTOP.HOUR_END));
            properties.setProperty("event.top.minute_end", String.valueOf(ConstDataEventTOP.MIN_END));

            properties.setProperty("event.top.month_reward", String.valueOf(ConstDataEventTOP.MONTH_REWARD));
            properties.setProperty("event.top.date_reward", String.valueOf(ConstDataEventTOP.DATE_REWARD));
            properties.setProperty("event.top.hour_reward", String.valueOf(ConstDataEventTOP.HOUR_REWARD));
            properties.setProperty("event.top.minute_reward", String.valueOf(ConstDataEventTOP.MIN_REWARD));

            // Ghi đè file
            try (FileOutputStream out = new FileOutputStream(path)) {
                properties.store(out, "Updated via Server Manager UI");
            }
            System.out.println(">> Config saved: Year " + ConstDataEventSM.YEAR_EVENT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean updateClanLevel(Clan clan) {
        String sql = "UPDATE clan SET level = ? WHERE id = ?";
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, clan.level);
            ps.setInt(2, clan.id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            Logger.logException(Manager.class, e,
                    "Error update clan level id " + clan.id);
            return false;
        }
    }
}
