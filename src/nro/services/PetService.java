package nro.services;

/*
 *
 *  Box ZALO: https://zalo.me/g/hfaysi616
 *  SDT ZALO: 0372875491
 *  Chuyên chỉnh sửa, mua bán source NRO,...
 */
import consts.ConstPlayer;
import nro.player.NewPet;
import nro.player.Pet;
import nro.player.Player;
import services.func.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

/**
 * Quản lý tạo, thay đổi, xóa, và chỉnh sửa Pet (Đệ tử)
 *
 * @author Mr
 */
public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    // ===========================================================
    // ======================= CREATE PET ========================
    // ===========================================================
    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, false, false, false, false, gender, "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    public void createNormalPet(Player player, byte... limitPower) {
        createPetThread(player, false, false, false, false, player.gender, "Xin hãy thu nhận tao làm đệ tử",
                limitPower);
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, true, false, false, false, gender, "Oa oa oa...", limitPower);
    }

    public void createMabuPet(Player player, byte... limitPower) {
        createPetThread(player, true, false, false, false, player.gender, "Oa oa oa...", limitPower);
    }

    public void createBlackGokuPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, false, true, false, false, gender,
                "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    public void createCellPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, false, false, true, false, gender,
                "Hãy hợp tác với ta, Kakarot!", limitPower);
    }

    public void createBerusPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, false, false, false, true, gender,
                "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    private void createPetThread(Player player, boolean mabu, boolean BlackGoku, boolean Cell, boolean Berus,
            int gender, String chat, byte... limitPower) {
        Thread.startVirtualThread(() -> {
            try {
                createNewPet(player, mabu, BlackGoku, Cell, Berus, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, chat);
            } catch (Exception ignored) {
            }
        });
    }

    // ===========================================================
    // ====================== CHANGE PET =========================
    // ===========================================================
    private void resetOldPet(Player player) {
        if (player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
    }

    public void changeNormalPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createNormalPet(player, gender, limitPower);
    }

    public void changeNormalPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createNormalPet(player, limitPower);
    }

    public void changeMabuPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createMabuPet(player, limitPower);
    }

    public void changeMabuPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createMabuPet(player, gender, limitPower);
    }

    public void changeBlackGokuPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        int gender = player.pet.gender;

        resetOldPet(player);

        createBlackGokuPet(player, gender, limitPower);
    }

    public void changeCellPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        int gender = player.pet.gender;

        resetOldPet(player);

        createCellPet(player, gender, limitPower);
    }

    public void changeBerusPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        int gender = player.pet.gender;

        resetOldPet(player);

        createBerusPet(player, gender, limitPower);
    }

    // ===========================================================
    // ====================== DELETE PET =========================
    // ===========================================================
    public void deletePet(Player player) {
        if (player.pet != null) {
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        }
    }

    // ===========================================================
    // ======================= RENAME PET ========================
    // ===========================================================
    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            }
            if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            }
            if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }

            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);

            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception ignored) {
                }
            });

        } catch (Exception ignored) {
        }
    }

    // ===========================================================
    // ======================= PET DATA ==========================
    // ===========================================================
    private int[] getDataPetNormal() {
        return new int[] { Util.nextInt(40, 105) * 20, Util.nextInt(40, 105) * 20, Util.nextInt(20, 45),
                Util.nextInt(9, 50), Util.nextInt(0, 2) };
    }

    private int[] getDataPetMabu() {
        return new int[] { Util.nextInt(40, 105) * 20, Util.nextInt(40, 105) * 20, Util.nextInt(50, 120),
                Util.nextInt(9, 50), Util.nextInt(0, 2) };
    }

    private int[] getDataPetBlackGoku() {
        return getDataPetMabu();
    }

    private int[] getDataPetCell() {
        return getDataPetMabu();
    }

    private int[] getDataPetBerus() {
        return getDataPetMabu();
    }

    // ===========================================================
    // =================== CREATE NEW PET CORE ===================
    // ===========================================================
    private void createNewPet(Player player, boolean isMabu, boolean isBlackGoku, boolean isCell, boolean isBerus,
            byte... gender) {
        int[] data = isMabu ? getDataPetMabu()
                : isBlackGoku ? getDataPetBlackGoku()
                        : isCell ? getDataPetCell()
                                : isBerus ? getDataPetBerus() : getDataPetNormal();

        Pet pet = new Pet(player);
        pet.name = "$"
                + (isMabu ? "Mabư" : isBlackGoku ? "BlackGoku" : isCell ? "Cell" : isBerus ? "Berus" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;

        pet.nPoint.power = (isBlackGoku || isCell || isBerus) ? 1_500_000L : isMabu ? 1_500_000L : 2000L;
        pet.typePet = (byte) (isMabu ? 1 : isBlackGoku ? 2 : isCell ? 3 : isBerus ? 4 : 0);

        pet.nPoint.stamina = pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];

        int itemBodySize = (pet.typePet >= 2) ? 7 : 7;
        for (int i = 0; i < itemBodySize; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }

        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 6; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }

        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    // ===========================================================
    // ==================== PET 2 (NEWPET) =======================
    // ===========================================================
    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newPet != null) {
            pl.newPet.dispose();
        }

        pl.newPet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newPet.name = "$";
        pl.newPet.gender = pl.gender;

        pl.newPet.nPoint.tiemNang = 1;
        pl.newPet.nPoint.power = 1;
        pl.newPet.nPoint.limitPower = 1;
        pl.newPet.nPoint.hpg = 500_000_000;
        pl.newPet.nPoint.mpg = 500_000_000;
        pl.newPet.nPoint.hp = 500_000_000;
        pl.newPet.nPoint.mp = 500_000_000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;

        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
    }
}
