package svenhjol.meson.helper;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import svenhjol.meson.iface.IMesonEnchantment.ICurse;
import svenhjol.meson.Meson;

import java.util.*;

public class EnchantmentHelper extends net.minecraft.enchantment.EnchantmentHelper
{
    private static List<Enchantment> availableCurses = new ArrayList<>();

    public static void addAvailableCurses(Enchantment... curses)
    {
        availableCurses.addAll(Arrays.asList(curses));
    }

    public static List<Enchantment> getItemCurses(ItemStack item)
    {
        NBTTagList tags = item.getEnchantmentTagList();
        List<Enchantment> curses = new ArrayList<>();

        if (!tags.isEmpty()) {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(item);

            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if (enchantment == null) continue;
                if (enchantment.isCurse() || enchantment instanceof ICurse) {
                    curses.add(enchantment);
                }
            }
        }

        return curses;
    }

    public static boolean hasEnchantment(Enchantment enchantment, ItemStack item)
    {
        if (item.getItem() instanceof ItemEnchantedBook) {
            // enchanted books have to be handled separately
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(item);
            for (int i = 0; i < enchantments.tagCount(); ++i) {
                NBTTagCompound nbttagcompound = enchantments.getCompoundTagAt(i);
                Enchantment e = Enchantment.getEnchantmentByID(nbttagcompound.getShort("id"));
                if (e == enchantment) {
                    return true;
                }
            }
        } else {
            // general item enchants
            return getEnchantmentLevel(enchantment, item) > 0;
        }

        return false;
    }

    public static boolean isItemCursed(ItemStack item)
    {
        return getItemCurses(item).size() > 0;
    }

    public static void applyRandomCurse(EntityPlayer player)
    {
        List<Enchantment> curses = new ArrayList<>();
        curses.add(Enchantments.VANISHING_CURSE);
        curses.add(Enchantments.BINDING_CURSE);
        curses.addAll(availableCurses);
        Enchantment curse = curses.get(new Random().nextInt(curses.size()));

        List<ItemStack> curseableItems = new ArrayList<>();
        ItemStack itemToCurse = null;
        NonNullList<ItemStack> armorInventory = player.inventory.armorInventory;
        NonNullList<ItemStack> mainInventory = player.inventory.mainInventory;
        NonNullList<ItemStack> useInventory = null;

        if (curse != null && curse.type != null) {
            switch (curse.type) {
                case ARMOR:
                case WEARABLE:
                    useInventory = armorInventory;
                    break;

                case WEAPON:
                case DIGGER:
                    useInventory = mainInventory;
                    break;

                case ALL:
                default:
                    if (new Random().nextInt(2) == 0) {
                        useInventory = mainInventory;
                    } else {
                        boolean hasArmor = false;
                        for (ItemStack stack : armorInventory) {
                            if (stack.isEmpty()) continue;
                            hasArmor = true;
                            break;
                        }
                        useInventory = hasArmor ? armorInventory : mainInventory;
                    }
                    break;
            }
        }


        if (useInventory != null) {
            for (ItemStack item : useInventory) {
                if (!item.isEmpty()
                    && !EnchantmentHelper.isItemCursed(item)
                    && (item.getItem() instanceof ItemEnchantedBook || item.isItemEnchanted() || item.isItemEnchantable())
                ) {
                    if (item.getItem() instanceof ItemEnchantedBook && EnchantmentHelper.getEnchantments(item).size() > 2) continue;
                    if (!curse.canApplyAtEnchantingTable(item)) continue; // don't allow items that are explicitly not allowed to be cursed
                    curseableItems.add(item);
                }
            }
        }

        if (!curseableItems.isEmpty()) {
            itemToCurse = curseableItems.get(new Random().nextInt(curseableItems.size()));
        }

        if (itemToCurse != null) {
            Map<Enchantment, Integer> existing = getEnchantments(itemToCurse);
            existing.put(curse, 1);
            setEnchantments(existing, itemToCurse);
        } else {
            Meson.debug("No item to curse!");
        }
    }

    public static void removeRandomCurse(ItemStack stack)
    {
        List<Enchantment> curses = getItemCurses(stack);
        if (curses.size() > 0) {
            Enchantment curseToRemove = curses.get(new Random().nextInt(curses.size()));
            removeEnchantment(curseToRemove, stack);
        }
    }

    public static void removeEnchantment(Enchantment enchantment, ItemStack stack)
    {
        if (hasEnchantment(enchantment, stack)) {
            Map<Enchantment, Integer> enchants = getEnchantments(stack);
            enchants.remove(enchantment);
            EnchantmentHelper.setEnchantments(enchants, stack);
        }
    }
}
