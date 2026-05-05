package models.Combine;

import consts.ConstNpc;
import item.Item;

import java.io.IOException;

import models.Combine.manifest.ChampaBanDoRac;
import models.Combine.manifest.CheTaoManhThienSu;
import models.Combine.manifest.CheTaoTrangBiThanhQuang;
import models.Combine.manifest.CheTaoTrangBiThienSu;
import models.Combine.manifest.CuongHoaChanMenh;
import models.Combine.manifest.CuongHoaChienHon;
import models.Combine.manifest.CuongHoaLoSaoPhaLe;
import models.Combine.manifest.CuongHoaSachTuyetKy;
import models.Combine.manifest.CuongHoaTrangBi;
import models.Combine.manifest.DanhBongSaoPhaLe;
import models.Combine.manifest.DapDoAoHoa;
import models.Combine.manifest.EpSaoTrangBi;
import models.Combine.manifest.GiaHanVatPham;
import models.Combine.manifest.GiamDinhSach;
import models.Combine.manifest.HoiPhucSach;
import models.Combine.manifest.KichHoatTrangBiThanhQuang;
import models.Combine.manifest.MoKhoaItem;
import models.Combine.manifest.NangCapBongTai;
import models.Combine.manifest.NangCapBongTaiCap3;
import models.Combine.manifest.NangCapCaiTrang;
import models.Combine.manifest.NangCapChanMenh;
import models.Combine.manifest.NangCapKichHoat;
import models.Combine.manifest.NangCapKichHoatThienSu;
import models.Combine.manifest.NangCapKichHoatVip;
import models.Combine.manifest.NangCapSachTuyetKy;
import models.Combine.manifest.NangCapSaoPhaLe;
import models.Combine.manifest.NangCapVatPham;
import models.Combine.manifest.NangChiSoBongTai;
import models.Combine.manifest.NangChiSoBongTaiCap3;
import models.Combine.manifest.NangGiapLuyenTap;
import models.Combine.manifest.NhapNgocRong;
import models.Combine.manifest.PhaLeHoaTrangBi;
import models.Combine.manifest.PhanRaSach;
import models.Combine.manifest.PhanRaTrangBiKichHoat.PhanRaTrangBi;
import models.Combine.manifest.PhanRaDoThanLinh;
import models.Combine.manifest.PhapSuHoa;
import models.Combine.manifest.RemoveOptionItem;
import models.Combine.manifest.SieuHoaCaiTrang;
import models.Combine.manifest.TaiTaoCapsuleKichHoat;
import models.Combine.manifest.TaiTaoTrangBiKichHoat;
import models.Combine.manifest.TaoDaHematite;
import models.Combine.manifest.TaySach;
import models.Combine.manifest.TinhAnTrangBi;
import models.Combine.manifest.TinhThachHoa;
import nro.player.Player;
import network.Message;
import nro.models.npc.Npc;
import nro.models.npc.NpcManager;
import nro.services.InventoryService;

public class CombineService {

    private static final int COST = 500000000;
    private static final int TIME_COMBINE = 1500;
    public static final byte MAX_STAR_ITEM = 8;
    public static final byte MAX_LEVEL_ITEM = 12;
    private static final byte OPEN_TAB_COMBINE = 0;
    private static final byte REOPEN_TAB_COMBINE = 1;
    private static final byte combineSUCCESS = 2;
    private static final byte combineFAIL = 3;
    private static final byte combineCHANGE_OPTION = 4;
    private static final byte combineDRAGON_BALL = 5;
    public static final byte OPEN_ITEM = 6;
    public static final int EP_SAO_TRANG_BI = 500;
    public static final int PHA_LE_HOA_TRANG_BI = 501;
    public static final int CHUYEN_HOA_TRANG_BI_DUNG_VANG = 502;
    public static final int CHUYEN_HOA_TRANG_BI_DUNG_NGOC = 503;
    public static final int NHAP_DA = 504;
    public static final int NANG_CAP_SAO_PHA_LE = 100;
    public static final int DANH_BONG_SAO_PHA_LE = 101;
    public static final int CUONG_HOA_LO_SAO_PHA_LE = 102;
    public static final int TAO_DA_HEMATITE = 103;
    public static final int GIAM_DINH_SACH = 104;
    public static final int TAY_SACH = 105;
    public static final int NANG_CAP_SACH_TUYET_KY = 106;
    public static final int HOI_PHUC_SACH = 107;
    public static final int PHAN_RA_SACH = 108;
    public static final int CHE_TAO_TRANG_BI_THIEN_SU = 109;
    public static final int NANG_CAP_VAT_PHAM = 510;
    public static final int NANG_CAP_BONG_TAI = 511;
    public static final int LAM_PHEP_NHAP_DA = 512;
    public static final int NHAP_NGOC_RONG = 513;
    public static final int NANG_CHI_SO_BONG_TAI = 517;;
    public static final int NANG_CAP_KICH_HOAT = 518;
    public static final int NANG_CAP_KICH_HOAT_VIP = 519;
    public static final int NANG_CAP_CAI_TRANG = 1912;
    public static final int CUONG_HOA_LINH_VUC = 1999;
    public static final int CUONG_HOA_HAO_QUANG = 2000;
    public static final int CUONG_HOA_CHIEN_HON = 2001;
    public static final int NANG_CAP_KICH_HOAT_THIEN_SU = 2002;

    public static final int DAP_DO_AO_HOA = 520;
    public static final int PS_HOA_TRANG_BI = 521;
    public static final int TAY_PS_HOA_TRANG_BI = 522;
    public static final int MO_KHOA_ITEM = 523;
    public static final int NANG_CAP_CHAN_MENH = 524;
    public static final int AN_TRANG_BI = 525;
    public static final int GIA_HAN_VAT_PHAM = 526;
    public static final int SIEU_HOA = 527;
    public static final int TINH_THACH_HOA = 528;
    public static final int NANG_GIAP_LUYEN_TAP = 529;
    public static final int TAI_TAO_TRANG_BI_KICH_HOAT = 530;
    public static final int NANG_CHI_SO_BONG_TAI_CAP_3 = 531;
    public static final int NANG_CAP_BONG_TAI_CAP_3 = 532;

// thêm 2 dòng này
      public static final int NANG_OPTION_1_BONG_TAI_CAP_3 = 540;
      public static final int NANG_OPTION_2_BONG_TAI_CAP_3 = 541;
      public static final int PHAN_RA_TRANG_BI_KH = 996;
      public static final int TAI_TAO_CAPSULE_KH = 997;
      public static final int CUONG_HOA_CHAN_MENH = 533;
      public static final int CUONG_HOA_SACH_TUYET_KY = 534;
      public static final int CUONG_HOA_TRANG_BI = 535;
      public static final int CHE_TAO_MANH_THIEN_SU = 995;
      public static final int CHE_TAO_TRANG_BI_THANH_QUANG = 998;
      public static final int KICH_HOAT_TRANG_BI_THANH_QUANG = 999;
      public static final int PHAN_RA_DO_THAN_LINH = 536;
      public static final int CHAMPA_BAN_DO_RAC = 600;  // Champa — Panel bán đồ rác
      public static final int CHAMPA_HIEN_TE = 601;     // Champa — Hiến tế trang bị

    private static CombineService instance;

    public final Npc baHatMit;
    public final Npc whis;

    private CombineService() {
        this.baHatMit = NpcManager.getNpc(ConstNpc.BA_HAT_MIT);
        this.whis = NpcManager.getNpc(ConstNpc.WHIS);
    }

    public static CombineService gI() {
        if (instance == null) {
            instance = new CombineService();
        }
        return instance;
    }

    /**
     * Hiển thị thông tin đập đồ
     *
     * @param player
     * @param index
     */
    public void showInfoCombine(Player player, int[] index) {
        if (player.combine == null) {
            return;
        }
        if (player.combine.typeCombine != CHE_TAO_TRANG_BI_THANH_QUANG) {
            player.combine.clearItemCombine();
        }
        if (index.length > 0) {
            for (int i = 0; i < index.length; i++) {
                player.combine.itemsCombine.add(player.inventory.itemsBag.get(index[i]));
            }
        }
        switch (player.combine.typeCombine) {
            case EP_SAO_TRANG_BI ->
                EpSaoTrangBi.showInfoCombine(player);
            case PHA_LE_HOA_TRANG_BI ->
                PhaLeHoaTrangBi.showInfoCombine(player);
            case NHAP_NGOC_RONG ->
                NhapNgocRong.showInfoCombine(player);
            case NANG_CAP_VAT_PHAM ->
                NangCapVatPham.showInfoCombine(player);
            case NANG_CAP_BONG_TAI ->
                NangCapBongTai.showInfoCombine(player);
            case NANG_CAP_BONG_TAI_CAP_3 ->
                NangCapBongTaiCap3.showInfoCombine(player);
            case NANG_CHI_SO_BONG_TAI ->
                NangChiSoBongTai.showInfoCombine(player);
            case NANG_CHI_SO_BONG_TAI_CAP_3 -> // day la them nang chi so btc3
                NangChiSoBongTaiCap3.showInfoCombine(player);
            case NANG_OPTION_1_BONG_TAI_CAP_3 ->
                NangChiSoBongTaiCap3.showInfoCombine(player);
            case NANG_OPTION_2_BONG_TAI_CAP_3 ->
                NangChiSoBongTaiCap3.showInfoCombine(player);
            case NANG_CAP_SAO_PHA_LE ->
                NangCapSaoPhaLe.showInfoCombine(player);
            case DANH_BONG_SAO_PHA_LE ->
                DanhBongSaoPhaLe.showInfoCombine(player);
            case NANG_CAP_CAI_TRANG -> NangCapCaiTrang.showInfoCombine(player);
            case CUONG_HOA_LINH_VUC ->
                models.Combine.manifest.CuongHoaLinhVuc.showInfoCombine(player);
            case CUONG_HOA_HAO_QUANG ->
                models.Combine.manifest.CuongHoaHaoQuang.showInfoCombine(player);
            case CUONG_HOA_CHIEN_HON ->
                CuongHoaChienHon.showInfoCombine(player);

            case CUONG_HOA_LO_SAO_PHA_LE ->
                CuongHoaLoSaoPhaLe.showInfoCombine(player);
            case TAO_DA_HEMATITE ->
                TaoDaHematite.showInfoCombine(player);
            case GIAM_DINH_SACH ->
                GiamDinhSach.showInfoCombine(player);
            case TAY_SACH ->
                TaySach.showInfoCombine(player);
            case NANG_CAP_SACH_TUYET_KY ->
                NangCapSachTuyetKy.showInfoCombine(player);
            case HOI_PHUC_SACH ->
                HoiPhucSach.showInfoCombine(player);
            case PHAN_RA_SACH ->
                PhanRaSach.showInfoCombine(player);
            case CHE_TAO_TRANG_BI_THIEN_SU ->
                CheTaoTrangBiThienSu.showInfoCombine(player);
            case NANG_CAP_KICH_HOAT ->
                NangCapKichHoat.showInfoCombine(player);
            case NANG_CAP_KICH_HOAT_VIP ->
                NangCapKichHoatVip.showInfoCombine(player);
            case NANG_CAP_KICH_HOAT_THIEN_SU ->
                NangCapKichHoatThienSu.showInfoCombine(player);
            case DAP_DO_AO_HOA ->
                DapDoAoHoa.showInfoCombine(player);
            case PS_HOA_TRANG_BI ->
                PhapSuHoa.showInfoCombine(player);
            case TAY_PS_HOA_TRANG_BI ->
                RemoveOptionItem.showInfoCombine(player);
            case MO_KHOA_ITEM ->
                MoKhoaItem.showInfoCombine(player);
            case NANG_CAP_CHAN_MENH ->
                NangCapChanMenh.showInfoCombine(player);
            case AN_TRANG_BI ->
                TinhAnTrangBi.showInfoCombine(player);
            case GIA_HAN_VAT_PHAM ->
                GiaHanVatPham.showInfoCombine(player);
            case SIEU_HOA ->
                SieuHoaCaiTrang.showInfoCombine(player);
            case TINH_THACH_HOA ->
                TinhThachHoa.showInfoCombine(player);
            case NANG_GIAP_LUYEN_TAP ->
                NangGiapLuyenTap.showInfoCombine(player);
            case TAI_TAO_TRANG_BI_KICH_HOAT ->
                TaiTaoTrangBiKichHoat.showInfoCombine(player);
            case PHAN_RA_TRANG_BI_KH ->
                PhanRaTrangBi.showInfoCombine(player);
            case TAI_TAO_CAPSULE_KH ->
                TaiTaoCapsuleKichHoat.showInfoCombine(player);
            case CUONG_HOA_TRANG_BI ->
                CuongHoaTrangBi.showInfoCombine(player);
            case CUONG_HOA_CHAN_MENH ->
                CuongHoaChanMenh.showInfoCombine(player);
            case CUONG_HOA_SACH_TUYET_KY ->
                CuongHoaSachTuyetKy.showInfoCombine(player);
            case CHE_TAO_MANH_THIEN_SU ->
                CheTaoManhThienSu.showInfoCombine(player);
            case CHE_TAO_TRANG_BI_THANH_QUANG ->
                CheTaoTrangBiThanhQuang.showInfoCombine(player);
            case KICH_HOAT_TRANG_BI_THANH_QUANG ->
                KichHoatTrangBiThanhQuang.showInfoCombine(player);
            case PHAN_RA_DO_THAN_LINH ->
                PhanRaDoThanLinh.showInfoCombine(player);
            case CHAMPA_BAN_DO_RAC ->
                models.Combine.manifest.ChampaBanDoRac.showInfoCombine(player);
            case CHAMPA_HIEN_TE ->
                models.Combine.manifest.ChampaHienTe.showInfoCombine(player);

        }
    }

    /**
     * Bắt đầu đập đồ - điều hướng từng loại đập đồ
     *
     * @param player
     * @param n
     */
    public void startCombine(Player player, int... n) {
        int num = 0;
        if (n.length > 0) {
            num = n[0];
        }
        switch (player.combine.typeCombine) {
            case EP_SAO_TRANG_BI ->
                EpSaoTrangBi.epSaoTrangBi(player);
            case PHA_LE_HOA_TRANG_BI ->
                PhaLeHoaTrangBi.phaLeHoa(player, num);
            case NHAP_NGOC_RONG ->
                NhapNgocRong.nhapNgocRong(player, num == 1);
            case NANG_CAP_VAT_PHAM ->
                NangCapVatPham.nangCapVatPham(player, num == 1);
            case NANG_CAP_BONG_TAI ->
                NangCapBongTai.nangCapBongTai(player);
            case NANG_CAP_BONG_TAI_CAP_3 ->
                NangCapBongTaiCap3.nangCapBongTai(player);
            case NANG_CHI_SO_BONG_TAI ->
                NangChiSoBongTai.nangChiSoBongTai(player);
           case NANG_CHI_SO_BONG_TAI_CAP_3 ->
                NangChiSoBongTaiCap3.nangChiSoBongTai(player, 1);
           case NANG_OPTION_1_BONG_TAI_CAP_3 ->
                NangChiSoBongTaiCap3.nangChiSoBongTai(player, 1);
           case NANG_OPTION_2_BONG_TAI_CAP_3 ->
                NangChiSoBongTaiCap3.nangChiSoBongTai(player, 2);
            case NANG_CAP_SAO_PHA_LE ->
                NangCapSaoPhaLe.nangCapSaoPhaLe(player);
            case DANH_BONG_SAO_PHA_LE ->
                DanhBongSaoPhaLe.danhBongSaoPhaLe(player);
            case NANG_CAP_CAI_TRANG -> NangCapCaiTrang.startCombine(player);
            case CUONG_HOA_LINH_VUC ->
                models.Combine.manifest.CuongHoaLinhVuc.startCombine(player);
            case CUONG_HOA_HAO_QUANG ->
                models.Combine.manifest.CuongHoaHaoQuang.startCombine(player);
            case CUONG_HOA_CHIEN_HON ->
                CuongHoaChienHon.startCombine(player);
            case CUONG_HOA_LO_SAO_PHA_LE ->
                CuongHoaLoSaoPhaLe.cuongHoaLoSaoPhaLe(player);
            case TAO_DA_HEMATITE ->
                TaoDaHematite.taoDaHematite(player);
            case GIAM_DINH_SACH ->
                GiamDinhSach.giamDinhSach(player);
            case TAY_SACH ->
                TaySach.taySach(player);
            case NANG_CAP_SACH_TUYET_KY ->
                NangCapSachTuyetKy.nangCapSachTuyetKy(player);
            case HOI_PHUC_SACH ->
                HoiPhucSach.hoiPhucSach(player);
            case PHAN_RA_SACH ->
                PhanRaSach.phanRaSach(player);
            case CHE_TAO_TRANG_BI_THIEN_SU ->
                CheTaoTrangBiThienSu.cheTaoTrangBiThienSu(player);
            case NANG_CAP_KICH_HOAT ->
                NangCapKichHoat.startCombine(player);
            case NANG_CAP_KICH_HOAT_VIP ->
                NangCapKichHoatVip.startCombine(player);
            case NANG_CAP_KICH_HOAT_THIEN_SU ->
                NangCapKichHoatThienSu.startCombine(player);
            case DAP_DO_AO_HOA ->
                DapDoAoHoa.startCombine(player);
            case PS_HOA_TRANG_BI ->
                PhapSuHoa.startCombine(player);
            case TAY_PS_HOA_TRANG_BI ->
                RemoveOptionItem.startCombine(player);
            case MO_KHOA_ITEM ->
                MoKhoaItem.startCombine(player);
            case NANG_CAP_CHAN_MENH ->
                NangCapChanMenh.startCombine(player);
            case AN_TRANG_BI ->
                TinhAnTrangBi.startCombine(player);
            case GIA_HAN_VAT_PHAM ->
                GiaHanVatPham.startCombine(player);
            case SIEU_HOA ->
                SieuHoaCaiTrang.startCombine(player);
            case TINH_THACH_HOA ->
                TinhThachHoa.startCombine(player);
            case NANG_GIAP_LUYEN_TAP ->
                NangGiapLuyenTap.startCombine(player);
            case TAI_TAO_TRANG_BI_KICH_HOAT ->
                TaiTaoTrangBiKichHoat.startCombine(player);
            case PHAN_RA_TRANG_BI_KH ->
                PhanRaTrangBi.ThucHienPhanRa(player);
            case TAI_TAO_CAPSULE_KH ->
                TaiTaoCapsuleKichHoat.thucHienTaiTao(player);
            case CUONG_HOA_TRANG_BI ->
                CuongHoaTrangBi.startCombine(player);
            case CUONG_HOA_CHAN_MENH ->
                CuongHoaChanMenh.startCombine(player);
            case CUONG_HOA_SACH_TUYET_KY ->
                CuongHoaSachTuyetKy.startCombine(player);
            case CHE_TAO_MANH_THIEN_SU ->
                CheTaoManhThienSu.cheTao(player);
            case CHE_TAO_TRANG_BI_THANH_QUANG ->
                CheTaoTrangBiThanhQuang.cheTao(player);
            case KICH_HOAT_TRANG_BI_THANH_QUANG ->
                KichHoatTrangBiThanhQuang.kichHoat(player);
            case PHAN_RA_DO_THAN_LINH ->
                PhanRaDoThanLinh.startCombine(player);
            case CHAMPA_BAN_DO_RAC ->
                models.Combine.manifest.ChampaBanDoRac.startCombine(player);
            case CHAMPA_HIEN_TE ->
                models.Combine.manifest.ChampaHienTe.startCombine(player);
        }

        player.iDMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        player.combine.clearParamCombine();
        player.combine.lastTimeCombine = System.currentTimeMillis();

    }

    /**
     * Mở tab đập đồ
     *
     * @param player
     * @param type   kiểu đập đồ
     */
    public void openTabCombine(Player player, int type) {
        player.combine.setTypeCombine(type);
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(OPEN_TAB_COMBINE);
            msg.writer().writeUTF(getTextInfoTabCombine(type));
            msg.writer().writeUTF(getTextTopTabCombine(type));
            if (player.iDMark.getNpcChose() != null) {
                msg.writer().writeShort(player.iDMark.getNpcChose().tempId);
            }
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng mở item
     *
     * @param player
     * @param icon1
     * @param icon2
     */
    public void sendEffectOpenItem(Player player, short icon1, short icon2) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(OPEN_ITEM);
            msg.writer().writeShort(icon1);
            msg.writer().writeShort(icon2);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void sendEffectCombineItem(Player player, byte type, short icon1, short icon2) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(type);
            switch (type) {
                case 0:
                    msg.writer().writeUTF("");
                    msg.writer().writeUTF("");
                    break;
                case 1:
                    msg.writer().writeByte(0);
                    msg.writer().writeByte(-1);
                    break;
                case 2: // success 0 eff 0
                case 3: // success 1 eff 0
                    break;
                case 4: // success 0 eff 1
                    msg.writer().writeShort(icon1);
                    break;
                case 5: // success 0 eff 2
                    msg.writer().writeShort(icon1);
                    break;
                case 6: // success 0 eff 3
                    msg.writer().writeShort(icon1);
                    msg.writer().writeShort(icon2);
                    break;
                case 7: // success 0 eff 4
                    msg.writer().writeShort(icon1);
                    break;
                case 8: // success 1 eff 4
                    break;
            }
            msg.writer().writeShort(-1); // id npc
            // msg.writer().writeShort(-1); // x
            // msg.writer().writeShort(-1); // y
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng đập đồ thành công
     *
     * @param player
     */
    public void sendEffectSuccessCombine(Player player) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(combineSUCCESS);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng đập đồ thất bại
     *
     * @param player
     */
    public void sendEffectFailCombine(Player player) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(combineFAIL);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Gửi lại danh sách đồ trong tab combine
     *
     * @param player
     */
    public void reOpenItemCombine(Player player) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(REOPEN_TAB_COMBINE);
            msg.writer().writeByte(player.combine.itemsCombine.size());
            for (Item it : player.combine.itemsCombine) {
                for (int j = 0; j < player.inventory.itemsBag.size(); j++) {
                    if (it == player.inventory.itemsBag.get(j)) {
                        msg.writer().writeByte(j);
                    }
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Hiệu ứng ghép ngọc rồng
     *
     * @param player
     * @param icon
     */
    public void sendEffectCombineDB(Player player, short icon) {
        Message msg = null;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(combineDRAGON_BALL);
            msg.writer().writeShort(icon);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void sendAddItemCombine(Player player, int npcId, Item... items) {
        Message msg;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("By BARCOLL");
            msg.writer().writeUTF("ENZEEFXNRO - Đẳng Cấp Là Mãi Mãi");
            msg.writer().writeShort(npcId);
            player.sendMessage(msg);
            msg.cleanup();
            msg = new Message(-81);
            msg.writer().writeByte(1);
            msg.writer().writeByte(items.length);
            for (Item item : items) {
                msg.writer().writeByte(InventoryService.gI().getIndexItemBag(player, item));
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendEffSuccessVip(Player player, int iconID) {
        Message msg;
        try {
            msg = new Message(-81);
            msg.writer().writeByte(7);
            msg.writer().writeShort(iconID);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendEffFailVip(Player player) {
        try {
            Message msg;
            msg = new Message(-81);
            msg.writer().writeByte(8);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    private String getTextTopTabCombine(int type) {
        return switch (type) {
            case EP_SAO_TRANG_BI -> {
                yield "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở nên mạnh mẽ";
            }
            case PHA_LE_HOA_TRANG_BI -> {
                yield "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở thành trang bị pha lê";
            }
            case CHUYEN_HOA_TRANG_BI_DUNG_VANG, CHUYEN_HOA_TRANG_BI_DUNG_NGOC -> {
                yield "Lưu ý trang bị mới\nphải hơn trang bị gốc\n1 bậc";
            }
            case NHAP_NGOC_RONG -> {
                yield "Ta sẽ phù phép\ncho 7 viên Ngọc Rồng\nthành 1 viên Ngọc Rồng cấp cao";
            }
            case NHAP_DA -> {
                yield "Ta sẽ phù phép\ncho 10 mảnh đá vụn\ntrở thành 1 đá nâng cấp";
            }
            case NANG_CAP_VAT_PHAM -> {
                yield "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở nên mạnh mẽ";
            }
            case NANG_CAP_BONG_TAI -> {
                yield "Ta sẽ phù phép\ncho bông tai Porata của ngươi\nthành cấp 2";
            }

            case NANG_CAP_BONG_TAI_CAP_3 -> {
                yield "Ta sẽ phù phép\ncho bông tai Porata +2 của ngươi\nthành +3";
            }
            case NANG_CHI_SO_BONG_TAI -> {
                yield "Ta sẽ phù phép\ncho bông tai Porata cấp 2 của ngươi\ncó 1 chỉ số ngẫu nhiên";
            }
            case NANG_CHI_SO_BONG_TAI_CAP_3 -> {
                yield "Ta sẽ phù phép\ncho bông tai Porata cấp 3 của ngươi\ncó 1 chỉ số ngẫu nhiên";
            }
            case NANG_CAP_SAO_PHA_LE -> {
                yield "Ta sẽ phù phép\nnâng cấp Sao Pha Lê\nthành cấp 2";
            }
            case CHE_TAO_TRANG_BI_THANH_QUANG ->
                "Ta sẽ chế tạo\ntrang bị Thanh Quang\nmạnh mẽ từ mảnh thiên sứ";

            case KICH_HOAT_TRANG_BI_THANH_QUANG ->
                "Ta sẽ kích hoạt\nsức mạnh tiềm ẩn\ncủa trang bị Thanh Quang";
            case CHE_TAO_MANH_THIEN_SU ->
                "Ta sẽ chế tạo\nmảnh thiên sứ\nchứa sức mạnh thần thánh";
            case CUONG_HOA_TRANG_BI ->
                "Trang bị chỉ thật sự mạnh\nkhi được khai mở tiềm năng";
            case CUONG_HOA_SACH_TUYET_KY ->
                "Tuyệt kỹ chỉ thật sự mạnh\nkhi được khai mở tiềm năng";
            case CUONG_HOA_CHAN_MENH ->
                "Chân mệnh là gốc rễ\nsức mạnh của ngươi\nTa có thể giúp nó mạnh hơn";
            case DANH_BONG_SAO_PHA_LE -> {
                yield "Đánh bóng\nSao pha lê cấp 2";
            }
            case NANG_CAP_CAI_TRANG -> {
                yield "Ta sẽ giúp ngươi\nnâng cấp cải trang\nlên VIP hơn";
            }
            case CUONG_HOA_LINH_VUC -> {
                yield "Ta sẽ dùng Đá nâng cấp\nđể cường hóa Linh Vực\nlên tầm cao mới";
            }
            case CUONG_HOA_HAO_QUANG -> {
                yield "Ta sẽ dùng Đá Hào Quang\nđể cường hóa Hào Quang\nlấp lánh hơn";
            }
            case CUONG_HOA_CHIEN_HON -> {
                yield "Ta sẽ dùng Đá cường hóa chiến hồn\nđể cường hóa Chiến Hồn\n xịn sò hơn";
            }
            case CUONG_HOA_LO_SAO_PHA_LE -> {
                yield "Cường hóa\nÔ Sao Pha Lê";
            }
            case TAO_DA_HEMATITE -> {
                yield "Ta sẽ phù phép\ntạo đá hematite";
            }
            case GIAM_DINH_SACH -> {
                yield "Ta sẽ phù phép\ngiám định sách đó cho ngươi";
            }
            case TAY_SACH -> {
                yield "Ta sẽ phù phép\ntẩy sách đó cho ngươi";
            }
            case NANG_CAP_SACH_TUYET_KY -> {
                yield "Ta sẽ phù phép\nnâng cấp Sách Tuyệt Kỹ cho ngươi";
            }
            case HOI_PHUC_SACH -> {
                yield "Ta sẽ phù phép\nphục hồi sách cho ngươi";
            }
            case PHAN_RA_SACH -> {
                yield "Ta sẽ phù phép\nphân rã sách đó cho ngươi";
            }
            case CHE_TAO_TRANG_BI_THIEN_SU -> {
                yield "Chế tạo\ntrang bị thiên sứ";
            }
            case LAM_PHEP_NHAP_DA -> {
                yield "Ta sẽ phù phép\n"
                        + "cho 10 mảnh đá vụn\n"
                        + "trở thành 1 đá nâng cấp";
            }
            case NANG_CAP_KICH_HOAT -> {
                yield "Ta sẽ phù phép\nchế tạo trang bị\nthành trang bị Kích Hoạt";
            }
            case NANG_CAP_KICH_HOAT_VIP -> {
                yield "Ta sẽ phù phép\nchế tạo trang bịnthành trang bị Kích Hoạt Vip";
            }
            case NANG_CAP_KICH_HOAT_THIEN_SU -> {
                yield "Ta sẽ phù phép\nchế tạo trang bị\nthành trang bị Kích Hoạt Vip Vip";
            }
            case GIA_HAN_VAT_PHAM -> {
                yield "Ta sẽ phù phép\ncho trang bị của ngươi\nthêm hạn sử dụng";
            }
            case SIEU_HOA -> {
                yield "Ta sẽ giúp con siêu hóa\n Cải trang";
            }
            case TINH_THACH_HOA -> {
                yield "Ta sẽ giúp con Tinh Thạch đồ";
            }
            case DAP_DO_AO_HOA -> {
                yield "Ta sẽ giúp ngươi ảo hóa đồ để có thuộc tính cao hơn";
            }
            case PS_HOA_TRANG_BI -> {
                yield "Pháp sư hóa pet, linh thú, ván bay";
            }
            case TAY_PS_HOA_TRANG_BI -> {
                yield "Tẩy đồ";
            }
            case MO_KHOA_ITEM -> {
                yield "Mở Khóa giao dịch Item";
            }
            case AN_TRANG_BI -> {
                yield "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở thành trang bị Ấn";
            }
            case TAI_TAO_TRANG_BI_KICH_HOAT -> {
                yield "Ta sẽ phù phép\n tái tạo thành 1 viên\n Cápsule kích hoạt tự chọn";
            }
            case NANG_CAP_CHAN_MENH -> {
                yield "Ta sẽ nâng cấp\ncho Chân Mệnh của ngươi\ntrở nên mạnh mẽ";
            }
            case PHAN_RA_TRANG_BI_KH -> {
                yield "Ta sẽ phù phép\nphân rã thành\nkhoáng tái chế cho ngươi";
            }
            case TAI_TAO_CAPSULE_KH -> {
                yield "Ta sẽ phù phép\ntái tạo thành 1 viên\nCapsule kích hoạt tự chọn";
            }
            case PHAN_RA_DO_THAN_LINH ->
                "Ta sẽ phù phép\nphân rã trang bị Thần Linh\nthành Đá Ngũ Sắc";
            case CHAMPA_BAN_DO_RAC ->
                "Ta là Champa!\nĐặt đồ rác vào đây\nta sẽ định giá cho ngươi";
            case CHAMPA_HIEN_TE ->
                "Ta là Champa!\nĐặt 1 trang bị vào đây\nta sẽ thực hiện hiến tế rủi ro!";
            default -> {
                yield "";
            }
        };
    }

    private String getTextInfoTabCombine(int type) {
        return switch (type) {
            case EP_SAO_TRANG_BI -> {
                yield "Vào hành trang\nChọn trang bị\n(Áo, quần, găng, giày hoặc rađa) có ô đặt sao pha lê\nChọn loại sao pha lê\nSau đó chọn 'Nâng cấp'";
            }
            case PHA_LE_HOA_TRANG_BI -> {
                yield "Vào hành trang\nChọn trang bị\n(Áo, quần, găng, giày hoặc rađa)\nSau đó chọn 'Nâng cấp'";
            }
            case CHUYEN_HOA_TRANG_BI_DUNG_VANG, CHUYEN_HOA_TRANG_BI_DUNG_NGOC -> {
                yield "Vào hành trang\nChọn trang bị gốc\n(Áo,quần,găng,giày hoặc rađa)\ntừ cấp [+4] trở lên\nChọn tiếp trang bị mới\nchưa nâng cấp cần nhập thể\nsau đó chọn 'Nâng cấp'";
            }
            case NHAP_NGOC_RONG -> {
                yield "Vào hành trang\nChọn 7 viên ngọc cùng sao\nSau đó chọn 'Làm phép'";
            }
            case NHAP_DA -> {
                yield "Vào hành trang\nChọn 10 mảnh đá vụn\nChọn 1 bình nước phép\n(mua tại Uron ở trạm tàu vũ trụ)\nSau đó chọn 'Làm phép'";
            }
            case NANG_CAP_VAT_PHAM -> {
                yield "Vào hành trang\nChọn trang bị\n(Áo,quần,găng,giày hoặc rađa)\nChọn loại đá để nâng cấp\nSau đó chọn 'Nâng cấp'";
            }
            case NANG_CAP_BONG_TAI -> {
                yield "Vào hành trang\nChọn bông tai Porata\nChọn mảnh bông tai để nâng cấp, số lượng 9999 cái\nSau đó chọn 'Nâng cấp'";
            }
            case NANG_CAP_BONG_TAI_CAP_3 -> {
                yield "Vào hành trang\nChọn bông tai Porata cấp 2\nChọn mảnh bông tai cấp 3 để nâng cấp, số lượng 20.000 cái\nSau đó chọn 'Nâng cấp'";
            }
            case NANG_CHI_SO_BONG_TAI -> {
                yield "Vào hành trang\nChọn bông tai Porata\nChọn mảnh hồn porata số lượng 99\ncái và đá xanh lam để nâng cấp.\nSau đó chọn 'Nâng cấp chỉ số'";
            }
            case NANG_CHI_SO_BONG_TAI_CAP_3 -> {
                yield "Vào hành trang\nChọn bông tai Porata cấp 3\nChọn mảnh hồn porata số lượng 99\ncái và đá xanh lam để nâng cấp.\nSau đó chọn 'Nâng cấp chỉ số'";
            }
            case NANG_CAP_SAO_PHA_LE -> {
                yield "Vào hành trang\nChọn đá Hematite\nChọn loại sao pha lê (cấp 1)\nSau đó chọn 'Nâng cấp'";
            }
            case DANH_BONG_SAO_PHA_LE -> {
                yield "Vào hành trang\nChọn loại sao pha lê cấp 2 có từ 2 viên trở lên\nChọn 1 đá mài\nSau đó chọn 'Đánh bóng'";
            }
            case NANG_CAP_CAI_TRANG -> {
                yield "Vào hành trang\nChọn 1 Cải trang cần nâng và 1 cải trang nguyên liệu\n'Nâng cấp'";
            }
            case CUONG_HOA_LINH_VUC -> {
                yield "Vào hành trang\nChọn Linh Vực và 1 Đá nâng cấp \nSau đó chọn 'Cường hóa'";
            }
            case CUONG_HOA_HAO_QUANG -> {
                yield "Vào hành trang\nChọn Hào Quang và 1 Đá hào quang \nSau đó chọn 'Nâng cấp'";
            }
            case CUONG_HOA_CHIEN_HON -> {
                yield "Vào hành trang\nChọn Chiến hồn và  Đá cường hóa Chiến hồn \nSau đó chọn 'Nâng cấp'";
            }
            case CUONG_HOA_LO_SAO_PHA_LE -> {
                yield "Vào hành trang\nChọn trang bị có Ô sao thứ 8 trở lên chưa cường hóa\nChọn đá Hematite\nChọn dùi đục\nSau đó chọn 'Cường hóa'";
            }
            case CUONG_HOA_CHAN_MENH ->
                "Vào hành trang\nChọn Chân Mệnh 9\nĐặt vào ấn nâng cấp\nTốn 1000 Hồng Ngọc\nNgẫu nhiên tăng SD, HP hoặc KI\nSau đó chọn 'Cường hóa'";
            case CUONG_HOA_SACH_TUYET_KY ->
                "Vào hành trang\nChọn Sách Tuyệt Kỹ 2\nĐặt vào ấn nâng cấp\nTốn 1000 Hồng Ngọc\nNgẫu nhiên tăng SD, HP hoặc KI\nSau đó chọn 'Cường hóa'";
            case CHE_TAO_TRANG_BI_THANH_QUANG ->
                "Vào hành trang\n"
                        + "Chọn 5 đồ thiên sứ\n"
                        + "(Áo, quần, giày, găng hoặc nhẫn) làm gốc và 4 món thiên sứ ngẫu nhiên làm nguyên liệu\n"
                        + "Sau đó chọn 'Chế tạo'\n"
                        + "Nhận trang bị Thanh Quang";

            case KICH_HOAT_TRANG_BI_THANH_QUANG ->
                "Vào hành trang\n"
                        + "Chọn 1 trang bị Thanh Quang\n"
                        + "Tốn 1000 Hồng Ngọc mỗi lần\n"
                        + "Random theo hành tinh";
            case CHE_TAO_MANH_THIEN_SU ->
                "Vào hành trang\n"
                        + "Chọn trang bị thiên sứ\n"
                        + "Sau đó chọn 'Chế tạo'";
            case CUONG_HOA_TRANG_BI ->
                "Chọn giáp luyện tập 4\nHoặc bông tai 4";
            case TAO_DA_HEMATITE -> {
                yield "Vào hành trang\nChọn 5 sao pha lê cấp 2 cùng màu\nChọn 'Tạo đá Hematite'";
            }
            case GIAM_DINH_SACH -> {
                yield "Vào hành trang chọn\n1 sách cần giám định";
            }
            case TAY_SACH -> {
                yield "Vào hành trang chọn\n1 sách cần tẩy";
            }
            case NANG_CAP_SACH_TUYET_KY -> {
                yield "Vào hành trang chọn\nSách Tuyệt Kỹ 1 cần nâng cấp và 10 Kìm bấm giấy";
            }
            case HOI_PHUC_SACH -> {
                yield "Vào hành trang chọn\nCác Sách Tuyệt Kỹ cần phục hồi";
            }
            case PHAN_RA_SACH -> {
                yield "Vào hành trang chọn\n1 sách cần phân rã";
            }
            case CHE_TAO_TRANG_BI_THIEN_SU -> {
                yield "Cần 1 công thức\nMảnh trang bị tương ứng\n1 đá nâng cấp (tùy chọn)\n1 đá may mắn (tùy chọn)";
            }
            case LAM_PHEP_NHAP_DA -> {
                yield "Vào hành trang\n"
                        + "Chọn 10 mảnh đá vụn\n"
                        + "Chọn 1 bình nước phép\n"
                        + "(mua tại Uron ở trạm tàu vũ trụ)\n"
                        + "Sau đó chọn 'Làm phép'";
            }
            case NANG_CAP_KICH_HOAT -> {
                yield "Vào hành trang\nChọn 3 trang bị thần linh\nSau đó chọn 'Nâng cấp'";
            }
            case NANG_CAP_KICH_HOAT_VIP -> {
                yield "Vào hành trang\nChọn 1 món thần linh\nvé hủy diệt cùng loại và 1 món hủy diệt bất kì\nSau đó chọn 'Nâng cấp'";
            }
            case NANG_CAP_KICH_HOAT_THIEN_SU -> {
                yield "Thêm 3 trang bị Hủy Diệt vào để nâng cấp\n"
                    + "Trang bị kích hoạt sẽ ra theo món đầu tiên bỏ vào";
}
            case DAP_DO_AO_HOA -> {
                yield "vào hành trang\nChọn trang bị\n(Áo, quần, găng, giày hoặc rađa)"
                        + "\nChọn loại đá quý để nâng cấp\n"
                        + "\nCó thể thêm đá bảo vệ để tránh tụt cấp\n"
                        + "Sau đó chọn 'Nâng cấp'";
            }
            case PS_HOA_TRANG_BI -> {
                yield "Vào hành trang\nChọn 1 trang bị có thể hắc hóa (pet, linh thú, chân mệnh, ván bay,..) và đá pháp sư \n "
                        + " để nâng cấp chỉ số pháp sư"
                        + "Chỉ cần chọn 'Nâng Cấp'";
            }
            case MO_KHOA_ITEM -> {
                yield "vào hành trang\nChọn 1 trang bị khóa giao dịch ( bông tai, item sự kiện, thỏi vàng,..) và Đá Hoàng Kim \n "
                        + " để mở khóa giao dịch Item"
                        + "Chỉ cần chọn 'Mở Khóa'";
            }

            case TAY_PS_HOA_TRANG_BI -> {
                yield "vào hành trang\nChọn 1 trang bị có thể tẩy ( trang bị,linh thú,pet,..) và đá tẩy \n "
                        + " để xoá nâng cấp chỉ số trang bị như sao pha lê đã ép, ....."
                        + "Chỉ cần chọn 'Nâng Cấp'";
            }

            case AN_TRANG_BI -> {
                yield "Vào hành trang\nChọn 1 Trang bị(Áo, Quần ,Giày ,Găng ,Rada) Hủy Diệt và 99 mảnh Ấn\nSau đó chọn 'Làm phép'\n--------\nTinh ấn (5 món +15%HP)\n Nhật ấn (5 món +15%KI\n Nguyệt ấn (5 món +15%SD)";
            }

            case GIA_HAN_VAT_PHAM -> {
                yield "Vào hành trang\n"
                        + "Chọn 1 trang bị có hạn sử dụng\n"
                        + "Chọn Đá Hoàng Kim\n"
                        + "Sau đó chọn 'Gia hạn'";
            }
            case SIEU_HOA -> {
                yield "Vào hành trang\n"
                        + "Chọn 1 Cải trang\n"
                        + "Chọn Đá Siêu Hóa\n"
                        + "Sau đó chọn 'Nâng Cấp'";
            }
            case TINH_THACH_HOA -> {
                yield "Vào hành trang\n"
                        + "Chọn 1 Vật Phẩm (Pet, Linh Thú, VPDL)\n"
                        + "Chọn 1 loại đá Tinh thạch\n"
                        + "Sau đó chọn 'Nâng Cấp'";
            }

            case NANG_GIAP_LUYEN_TAP -> {
                yield "Vào hành trang\n"
                        + "Chọn 1 Giáp luyện tập\n"
                        + "Chọn đá hổ phách\n"
                        + "Sau đó chọn 'Nâng Cấp'";
            }
            case TAI_TAO_TRANG_BI_KICH_HOAT -> {
                yield "Vào hành trang\nChọn 3 khoáng tái chế\nChọn 1 Cápsule vỡ\nSau đó chọn 'Tái tạo'";
            }
            case NANG_CAP_CHAN_MENH -> {
                yield "Vào hành trang\nChọn 1 Chân Mệnh\nvà x99 Sao Thiên Tử\nSau đó chọn 'Nâng cấp'";
            }
            case PHAN_RA_TRANG_BI_KH -> {
                yield "Vào hành trang\nChọn hay nhiều\nTrang bị kích hoạt cần rã\nSau đó chọn 'Phân rã'";
            }
            case TAI_TAO_CAPSULE_KH -> {
                yield "Vào hành trang\nChọn 3 khoáng tái chế\nChọn 1 Capsule vỡ\nSau đó chọn 'Tái tạo'";
            }
            case PHAN_RA_DO_THAN_LINH -> {
                yield "Vào hành trang\nChọn trang bị Thần Linh cần phân rã\nCó thể chọn nhiều món\nSau đó chọn 'Phân rã'";
            }
            case CHAMPA_BAN_DO_RAC -> {
                yield "Vào hành trang\nChọn đồ rác muốn bán\n(TB cấp 1-12, thức ăn, đá thường)\nSau đó chọn 'Bán'";
            }
            case CHAMPA_HIEN_TE -> {
                yield "Vào hành trang\nChọn 1 Trang bị\nCó thể thêm 1 Hộp SKH Thần Linh\nSau đó chọn 'Hiến tế'";
            }
            default -> {
                yield "";
            }
        };
    }

}
