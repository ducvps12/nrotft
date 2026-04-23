package services.func;

import Top.MyClanTopCDRD;
import Top.MyClanTopKhiGas;
import Top.MyClanTopKhoBau;
import Top.TopCDRD;
import Top.TopChiSoManager;
import Top.TopKhiGas;
import Top.TopKhoBau;
import Top.TopPowerManager;
import Top.TopTaskManager;
import Top.TopVnd;
import Top.Topbongmaster;
import Top.Topcapsuvip;
import Top.Tophalloween_master;
import Top.Tophopkeomaquy;
import Top.Tophopqua2010;
import Top.Tophopquathang9;
import Top.Tophopquathang9vip;
import Top.Tophopquatrungthuvip;
import Top.Tophoptrahoacuc;
import Top.Topkeo_halloween;
import Top.Toplongdentreo;
import Top.Topthiep_halloween;
import Top.Topthiepchucvip;
import clan.Clan;
import consts.ConstSQL;
import java.util.function.Function;

import java.io.IOException;

import jdbc.DBConnecter;
import nro.player.Player;
import nro.server.Manager;
import network.Message;
import utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jdbc.NDVDB;
import jdbc.daos.NDVSqlFetcher;

import matches.TOP;
import models.Template.Part;
import nro.services.ItemService;
import nro.services.TaskService;
import org.json.simple.JSONArray;
import utils.Util;

public class TopService {

    private static TopService instance;

    public static TopService gI() {
        if (instance == null) {
            instance = new TopService();
        }
        return instance;
    }

    public void updateTop() {
        if (Manager.timeRealTop + (10 * 60 * 1000) < System.currentTimeMillis()) {
            Manager.timeRealTop = System.currentTimeMillis();
            try (Connection con = DBConnecter.getConnectionServer()) {
                Manager.topNV = Manager.realTop(ConstSQL.TOP_NV, con);
                Manager.topDC = Manager.realTop(ConstSQL.TOP_DC, con);
                Manager.topVDST = Manager.realTop(ConstSQL.TOP_VDST, con);
                Manager.topWHIS = Manager.realTop(ConstSQL.TOP_WHIS, con);
                Manager.topSM = Manager.realTop(ConstSQL.TOP_SM, con);
                Manager.Topmaydam = Manager.realTop(ConstSQL.queryTopmaydam, con);
                Manager.topWHIS = Manager.realTop(ConstSQL.TOP_WHIS, con);
                Manager.topNap = Manager.realTop(ConstSQL.TOP_NAP, con);
                Manager.topDuaSM = Manager.realTop(ConstSQL.TOP_DUA_SM, con);
                Manager.topDuaNap = Manager.realTop(ConstSQL.TOP_DUA_NAP, con);
            } catch (Exception ignored) {
                Logger.error("Lỗi đọc top");
            }
        }
    }

    public static void showListTopVnd(Player player) {
        TopVnd.getInstance().load();
        List<Player> list = TopVnd.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Tổng nạp: " + Util.numberFormatLouis(top.danap));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopbongmaster(Player player) {
        Topbongmaster.getInstance().load();
        List<Player> list = Topbongmaster.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.bongmaster));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophopquathang9vip(Player player) {
        Tophopquathang9vip.getInstance().load();
        List<Player> list = Tophopquathang9vip.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hopquathang9vip));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophopquathang9(Player player) {
        Tophopquathang9.getInstance().load();
        List<Player> list = Tophopquathang9.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hopquathang9));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophopquatrungthuvip(Player player) {
        Tophopquatrungthuvip.getInstance().load();
        List<Player> list = Tophopquatrungthuvip.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hopquatrungthuvip));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListToplongdentreo(Player player) {
        Toplongdentreo.getInstance().load();
        List<Player> list = Toplongdentreo.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.longdentreo));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophoptrahoacuc(Player player) {
        Tophoptrahoacuc.getInstance().load();
        List<Player> list = Tophoptrahoacuc.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hoptrahoacuc));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophopkeomaquy(Player player) {
        Tophopkeomaquy.getInstance().load();
        List<Player> list = Tophopkeomaquy.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hopkeomaquy));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopthiep_halloween(Player player) {
        Topthiep_halloween.getInstance().load();
        List<Player> list = Topthiep_halloween.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.thiep_halloween));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopkeo_halloween(Player player) {
        Topkeo_halloween.getInstance().load();
        List<Player> list = Topkeo_halloween.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.keo_halloween));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophalloween_master(Player player) {
        Tophalloween_master.getInstance().load();
        List<Player> list = Tophalloween_master.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.halloween_master));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopcapsuvip(Player player) {
        Topcapsuvip.getInstance().load();
        List<Player> list = Topcapsuvip.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.capsuvip));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopthiepchucvip(Player player) {
        Topthiepchucvip.getInstance().load();
        List<Player> list = Topthiepchucvip.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.thiepchucvip));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTophopqua2010(Player player) {
        Tophopqua2010.getInstance().load();
        List<Player> list = Tophopqua2010.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Điểm: " + Util.numberFormatLouis(top.hopqua2010));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListTopPower(Player player) {
        TopPowerManager.getInstance().load();
        List<Player> list = TopPowerManager.getInstance().getList();
        list.sort((p1, p2) -> Long.compare(p2.nPoint.power, p1.nPoint.power));
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());

            for (int i = 0; i < Math.min(100, list.size()); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                String inputDateString = top.firstTimeLogin.toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date inputDate = dateFormat.parse(inputDateString);
                    Date currentDate = new Date();
                    long timeDifferenceInMillis = currentDate.getTime() - inputDate.getTime();
                    long giây = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giây));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                msg.writer().writeUTF("Sức mạnh: " + Util.numberFormatLouis(top.nPoint.power));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showListTopSuKien(Player player) {
        Message msg = null;
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(ConstSQL.TOP_SU_KIEN);
             ResultSet rs = ps.executeQuery()) {

            List<String[]> topList = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString("name");
                int diem = rs.getInt("diem_su_kien");
                byte gender = rs.getByte("gender");
                String itemsBody = rs.getString("items_body");
                topList.add(new String[]{name, String.valueOf(diem), String.valueOf(gender), itemsBody});
            }

            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 10 Sự Kiện");
            msg.writer().writeByte(topList.size());

            for (int i = 0; i < topList.size(); i++) {
                String[] data = topList.get(i);
                byte gender = Byte.parseByte(data[2]);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);

                // Parse items_body for display
                try {
                    JSONArray items = (JSONArray) org.json.simple.JSONValue.parse(data[3]);
                    if (items != null && items.size() > 0) {
                        JSONArray item0 = (JSONArray) org.json.simple.JSONValue.parse(items.get(0).toString());
                        short head = Short.parseShort(String.valueOf(item0.get(0)));
                        msg.writer().writeShort(head != -1 ? head : (gender == 0 ? 64 : gender == 1 ? 9 : 6));
                    } else {
                        msg.writer().writeShort(gender == 0 ? 64 : gender == 1 ? 9 : 6);
                    }
                } catch (Exception ex) {
                    msg.writer().writeShort(gender == 0 ? 64 : gender == 1 ? 9 : 6);
                }

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }

                // Body & Leg defaults
                msg.writer().writeShort(gender == 0 ? 0 : gender == 1 ? 1 : 2);
                msg.writer().writeShort(gender == 0 ? 6 : gender == 1 ? 7 : 8);

                msg.writer().writeUTF(data[0]); // name
                msg.writer().writeUTF(""); // time placeholder
                msg.writer().writeUTF("Điểm SK: " + data[1]);
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showTopClanKhoBau(Player player) {
        TopKhoBau.getInstance().load();
        List<Clan> list = TopKhoBau.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Clan clan = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt((int) clan.id);
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(clan.name);

                msg.writer().writeUTF("Lv: " + clan.levelDoneBanDoKhoBau + " Trong "
                        + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhBDKB) + " giây");

                msg.writer().writeUTF("Bang chủ " + clan.getLeader().name);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMyTopClanKhoBau(Player player) {
        if (player.clan != null) {
            MyClanTopKhoBau.getInstance().load(player.clan.getLeader().id);
            List<Player> list = MyClanTopKhoBau.getInstance().getList();
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Player pl = list.get(i);
                    msg.writer().writeInt(i + 1);
                    msg.writer().writeInt((int) pl.id);
                    msg.writer().writeShort(pl.getHead());
                    if (player.getSession().version >= 214) {
                        msg.writer().writeShort(-1);
                    }
                    msg.writer().writeShort(pl.getBody());
                    msg.writer().writeShort(pl.getLeg());
                    msg.writer().writeUTF(pl.nameClan);
                    msg.writer().writeUTF("Lv: " + pl.levelBDKBDone + " ("
                            + Util.convertSecondsToTime(pl.lastTimeUpdateTopBDKB) + ")");
                    msg.writer()
                            .writeUTF("Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeBDKBDone) + "]");
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(0);
                msg.writer().writeInt(0);
                msg.writer().writeInt(0);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");

                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showTopClanCDRD(Player player) {
        TopCDRD.getInstance().load();
        List<Clan> list = TopCDRD.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Clan clan = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt((int) clan.id);
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(clan.name);

                msg.writer().writeUTF("Lv: " + clan.levelDoneConDuongRanDoc + " Trong "
                        + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhCDRD) + " giây");

                msg.writer().writeUTF("Bang chủ " + clan.getLeader().name);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMyTopClanCDRD(Player player) {
        if (player.clan != null) {
            MyClanTopCDRD.getInstance().load(player.clan.getLeader().id);
            List<Player> list = MyClanTopCDRD.getInstance().getList();
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Player pl = list.get(i);
                    msg.writer().writeInt(i + 1);
                    msg.writer().writeInt((int) pl.id);
                    msg.writer().writeShort(pl.getHead());
                    if (player.getSession().version >= 214) {
                        msg.writer().writeShort(-1);
                    }
                    msg.writer().writeShort(pl.getBody());
                    msg.writer().writeShort(pl.getLeg());
                    msg.writer().writeUTF(pl.nameClan);
                    msg.writer().writeUTF("Lv: " + pl.levelCDRDDone + " ("
                            + Util.convertSecondsToTime(pl.lastTimeUpdateTopCDRD) + ")");
                    msg.writer()
                            .writeUTF("Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeCDRDDone) + "]");
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(0);
                msg.writer().writeInt(0);
                msg.writer().writeInt(0);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");

                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showTopClanKhiGas(Player player) {
        TopKhiGas.getInstance().load();
        List<Clan> list = TopKhiGas.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Clan clan = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt((int) clan.id);
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(clan.name);

                msg.writer().writeUTF("Lv: " + clan.levelDoneKhiGas + " Trong "
                        + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhKhiGas) + " giây");

                msg.writer().writeUTF("Bang chủ " + clan.getLeader().name);
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMyTopClanKhiGas(Player player) {
        if (player.clan != null) {
            MyClanTopKhiGas.getInstance().load(player.clan.getLeader().id);
            List<Player> list = MyClanTopKhiGas.getInstance().getList();
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Player pl = list.get(i);
                    msg.writer().writeInt(i + 1);
                    msg.writer().writeInt((int) pl.id);
                    msg.writer().writeShort(pl.getHead());
                    if (player.getSession().version >= 214) {
                        msg.writer().writeShort(-1);
                    }
                    msg.writer().writeShort(pl.getBody());
                    msg.writer().writeShort(pl.getLeg());
                    msg.writer().writeUTF(pl.nameClan);
                    msg.writer().writeUTF("Lv: " + pl.levelKhiGasDone + " ("
                            + Util.convertSecondsToTime(pl.lastTimeUpdateTopKhiGas) + ")");
                    msg.writer().writeUTF(
                            "Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeKhiGasDone) + "]");
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Message msg = null;
            try {
                msg = new Message(-96);
                msg.writer().writeByte(0);
                msg.writer().writeUTF("Thành tích bang");
                msg.writer().writeByte(0);
                msg.writer().writeInt(0);
                msg.writer().writeInt(0);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");

                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void showListTopTask(Player player) {
        TopTaskManager.getInstance().load();
        List<Player> list = TopTaskManager.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                msg.writer().writeUTF(NDVSqlFetcher.loadById(top.id).playerTask.taskMain.name);
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static String getTopNap() {
        StringBuffer sb = new StringBuffer("");
        PreparedStatement ps;
        ResultSet rs;
        try {
            Connection conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_NAP);
            conn.setAutoCommit(false);

            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                sb.append(i).append(".").append(rs.getString("name")).append(": ").append(rs.getString("danap"))
                        .append(" Đã Nạp\b");
                i++;
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String getTopSM() {
        StringBuffer sb = new StringBuffer("");
        PreparedStatement ps;
        ResultSet rs;
        try {
            Connection conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_SM);
            conn.setAutoCommit(false);

            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                sb.append(i).append(".").append(rs.getString("name")).append(": ").append(rs.getString("sm"))
                        .append(" Sức Mạnh\b");
                i++;
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String getTopQuocVuong() {
        StringBuffer sb = new StringBuffer("");
        PreparedStatement ps;
        ResultSet rs;
        try {
            Connection conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_QUOC_VUONG);
            conn.setAutoCommit(false);
            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                int id = rs.getInt("accountId");
                String username = rs.getString("name");
                sb.append(i).append(".").append(id).append("-").append(username).append(": sở hữu ")
                        .append(rs.getString("thoi_vang")).append(" ")
                        .append(ItemService.gI().getTemplate(consts.ConstTranhNgocNamek.ITEM_TRANH_NGOC).name)
                        .append("\b");
                i++;
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static void showListTop(Player player, int select) {
        List<TOP> tops = Manager.topNV;
        switch (select) {
            case 0 ->
                tops = Manager.topNV;
            case 1 ->
                tops = Manager.topDC;
            case 2 ->
                tops = Manager.topSM;
            case 3 ->
                tops = Manager.topWHIS;
            // case 4 ->
            // tops = Manager.topNap;
            case 4 ->
                tops = Manager.topVDST;
            case 5 ->
                tops = Manager.topDuaSM;
            // case 6 ->
            // tops = Manager.topDuaNap;

        }
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(tops.size());
            for (int i = 0; i < tops.size(); i++) {
                TOP top = tops.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.getName());
                switch (select) {
                    case 0 -> {
                        msg.writer()
                                .writeUTF(TaskService.gI().getTaskMainById(player, top.getNv()).name.substring(0,
                                        TaskService.gI().getTaskMainById(player, top.getNv()).name.length() > 20 ? 20
                                                : TaskService.gI().getTaskMainById(player, top.getNv()).name.length())
                                        + "...");
                        msg.writer().writeUTF(
                                TaskService.gI().getTaskMainById(player, top.getNv()).subTasks.get(top.getSubnv()).name
                                        + " - " + getTimeLeft(top.getLasttime()));
                    }
                    case 1 -> {
                        msg.writer().writeUTF("Chơi đồ " + top.getDicanh() + " lần");
                        msg.writer().writeUTF("Gia nhập juventus " + top.getJuventus() + " lần");
                    }
                    case 2 -> {
                        msg.writer().writeUTF("" + Util.numberToMoney(top.getPower()) + " Sức mạnh");
                        msg.writer().writeUTF("" + top.getPower() + " Sức mạnh");
                    }
                    case 3 -> {
                        msg.writer().writeUTF("LV:" + top.getLevel() + " với "
                                + Util.roundToTwoDecimals(top.getTime() / 1000d) + " giây");
                        msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                    }
                    case 4 -> {
                        msg.writer().writeUTF("" + Util.numberToMoney(top.getCash()) + " VNĐ");
                        msg.writer().writeUTF("" + top.getCash() + " VNĐ");
                    }
                    case 5 -> {
                        msg.writer().writeUTF("Đã thử thách " + top.getDivdst() + " Lần");
                        msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                    }
                    // case 6 -> {
                    // msg.writer().writeUTF("" + Util.numberToMoney(top.getCash()) + " VNĐ");
                    // msg.writer().writeUTF("" + top.getCash() + " VNĐ");
                    // }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static String getTimeLeft(long lastTime) {
        int secondsPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);

        if (secondsPassed > 86400) {
            return (secondsPassed / 86400) + " ngày trước";
        } else if (secondsPassed > 3600) {
            return (secondsPassed / 3600) + " giờ trước";
        } else if (secondsPassed > 60) {
            return (secondsPassed / 60) + " phút trước";
        } else {
            return secondsPassed + " giây trước";
        }
    }

    public static void showTopHP(Player player) {
        TopChiSoManager.getInstance().loadTopHP();
        List<Player> list = TopChiSoManager.getInstance().getList();

        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top HP");
            msg.writer().writeByte(list.size());

            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);

                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }

                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);

                msg.writer().writeUTF("Top chỉ số");
                msg.writer().writeUTF("HP: " + Util.numberFormatLouis(top.nPoint.hpMax));
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showTopKI(Player player) {
        TopChiSoManager.getInstance().loadTopKI();
        List<Player> list = TopChiSoManager.getInstance().getList();

        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top KI");
            msg.writer().writeByte(list.size());

            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);

                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }

                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);

                msg.writer().writeUTF("Top chỉ số");
                msg.writer().writeUTF("KI: " + Util.numberFormatLouis(top.nPoint.mpMax));
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showTopSD(Player player) {
        TopChiSoManager.getInstance().loadTopDame();
        List<Player> list = TopChiSoManager.getInstance().getList();

        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top Sức đánh");
            msg.writer().writeByte(list.size());

            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);

                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }

                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);

                msg.writer().writeUTF("Top chỉ số");
                msg.writer().writeUTF("Sức đánh: " + Util.numberFormatLouis(top.nPoint.dame));
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
