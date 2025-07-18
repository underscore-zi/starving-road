package us.ri0.starving.modules;

import com.example.us.ri0.starving.AddonTemplate;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.*;

public class Pather extends Module {
    private final Queue<BlockPos> instaQ = new LinkedList<>();
    private final Queue<BlockPos> breakQ = new LinkedList<>();
    private final Queue<BlockPos> pathQ = new LinkedList<>();
    private final HashSet<BlockPos> handled = new HashSet<>();

    private final Color innerColor = new Color(65, 30, 0, 40);
    private final Color outerColor = new Color(65, 30, 0, 200);

    public Pather() {
        super(AddonTemplate.CATEGORY, "Pather", "Paths any dirt blocks along the starving road.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for(BlockPos pos : getCloseDirt()) {
            handleDirt(pos);
        }
        tickQueues();
    }


    @EventHandler
    private void onRender(meteordevelopment.meteorclient.events.render.Render3DEvent event) {
        for(BlockPos pos : getCloseDirt()) {
            event.renderer.box(pos, innerColor, outerColor, ShapeMode.Both, 0);
        }
    }

    /**
     * Queues up any actions on the given dirt position
     * @param pos the position of the dirt block to target
     */
    void handleDirt(BlockPos pos) {
        if(!PlayerUtils.isWithinReach(pos)) return;
        if(handled.contains(pos)) return;
        handled.add(pos);

        var aboveState = mc.world.getBlockState(pos.up());
        if(!aboveState.isAir()) {
            if(aboveState.getCollisionShape(mc.world, pos.up()).isEmpty()) {
                if (BlockUtils.canInstaBreak(pos.up())) {
                    instaQ.add(pos.up());
                } else if (aboveState.getBlock() == Blocks.SNOW) {
                    // Snow layers will be instabroke if using Eff5 but otherwise put it in the break queue
                    breakQ.add(pos.up());
                }
            }

            // Can't path the block if its got a block above it
            return;
        }

        pathQ.add(pos);
    }

    /**
     * Performs any queued up actions for the tick
     */
    void tickQueues() {
        // Entire instaq can be handled at once
        while(!instaQ.isEmpty()) {
            BlockPos pos = instaQ.poll();
            BlockUtils.breakBlock(pos, false);
        }

        if(!breakQ.isEmpty()) {
            BlockPos pos = breakQ.peek();
            if(!PlayerUtils.isWithinReach(pos)) breakQ.poll();
            else {
                switchToShovel();
                if(!BlockUtils.breakBlock(pos, false)) {
                    // Remove from queue once breakBlock returns false
                    breakQ.poll();
                }
            }
            return;
        }

        if (!pathQ.isEmpty()) {
            if(!switchToShovel()) {
                ChatUtils.error("Unable to switch to shovel. Please ensure you have a shovel in your hotbar.");
                return;
            }
            BlockPos pos = pathQ.poll();
            var hitResult = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
            BlockUtils.interact(hitResult, Hand.MAIN_HAND, true);
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        // If we break a block or something remove it from the handled set so we can recheck it for any new actions
        handled.remove(event.pos);
        handled.remove(event.pos.down());
    }


    /**
     * Obtains a list of all reachable dirt blocks that are also the topmost dirt block in the world. As this is
     * only the topmost dirt it will not work when in a cave.
     */
    List<BlockPos> getCloseDirt() {
        var x = (int)Math.round(mc.player.getPos().getX());
        final int reach = 3;

        ArrayList<BlockPos> blocks = new ArrayList<>(reach * 2 * 2 + 2);
        for(int offset = -reach; offset <= reach; offset++) {
            var pos1 = getTopDirt(x + offset, 1533);
            if(pos1 != null && PlayerUtils.isWithinReach(pos1)) blocks.add(pos1);

            var pos2 = getTopDirt(x + offset, 1534);
            if(pos2 != null && PlayerUtils.isWithinReach(pos2)) blocks.add(pos2);
        }
        return blocks;
    }

    /**
     * Attempts to switch the player's mainhand to a shovel
     * @return true if successful false otherwise
     */
    private boolean switchToShovel() {
        var current = mc.player.getMainHandStack().getItem();
        if(current == Items.DIAMOND_SHOVEL || current == Items.NETHERITE_SHOVEL) {
            return true;
        }

        // Find a shovel in the inventory
        var slot = InvUtils.findInHotbar(Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL);
        if(!slot.found()) return false;

        // Swap to shovel
        InvUtils.swap(slot.slot(), false);

        return true;
    }

    /**
     * Checks if the given block is a dirt block
     * @param block the block to check
     * @return true if the block is a dirt block, false otherwise
     */
    public boolean isDirtBlock(Block block) {
        return block == Blocks.DIRT ||
            block == Blocks.COARSE_DIRT ||
            block == Blocks.PODZOL ||
            block == Blocks.MYCELIUM ||
            block == Blocks.GRASS_BLOCK;
    }

    /**
     * Gets the topmost dirt block at the given coordinates, assumes y > 0 so it won't work underground
     * @param x the x coordinate
     * @param z the z coordinate
     * @return the position of the topmost dirt block, or null if there is no dirt block
     */
    BlockPos getTopDirt(int x, int z) {
        for(int y = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z); y > 0; y--) {
            var pos = new BlockPos(x, y, z);
            var block = mc.world.getBlockState(pos).getBlock();
            if(isDirtBlock(block)) {
                return pos;
            }
            if(block == Blocks.DIRT_PATH) {
                return null;
            }
        }
        return null;
    }
}
