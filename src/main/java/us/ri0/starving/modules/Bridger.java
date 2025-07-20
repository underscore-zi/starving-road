package us.ri0.starving.modules;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import us.ri0.starving.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.LinkedList;

public class Bridger extends Module {
    public Bridger() {
        super(AddonTemplate.CATEGORY, "Bridger", "Scaffolding, but worse.");
    }

    private HashSet<BlockPos> recentPlacements = new HashSet<>();
    private final Color innerColor = new Color(150, 150, 150, 40);
    private final Color outerColor = new Color(150, 150, 150, 200);

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        final int reach = 3;
        final int maxPerTick = 7;
        int placed = 0;

        BlockPos[] blocks = targets(reach);
        if(blocks.length == 0) {
            return;
        }

        if(!switchToBuildMat()) {
            return;
        }

        var deltaY = mc.player.getPos().y - Math.floor(mc.player.getPos().y);
        boolean bottomSlab = deltaY >= 0.5 && deltaY < 0.90;

        for(BlockPos pos : blocks) {
            placeSlab(pos, bottomSlab);
            placed++;
            if(placed >= maxPerTick) {
                break;
            }
        }

        if(Math.random() < 0.001) {
            removeOldPlacements();
        }

    }

    private void removeOldPlacements() {
        // Remove placements that are too old
        recentPlacements.removeIf(pos -> {
            var distance = PlayerUtils.distanceTo(pos);
            return distance > 10; // Remove placements that are more than 10 blocks away
        });
    }

    private boolean placeSlab(BlockPos pos, boolean bottomSlab) {
        if(!PlayerUtils.isWithinReach(pos)) return false;
        if(recentPlacements.contains(pos)) return false; // Don't place the same slab twice
        recentPlacements.add(pos);

        Direction dir = bottomSlab ? Direction.UP : Direction.DOWN;
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.toCenterPos(),
            dir,
            pos,
            false
        );


        mc.player.networkHandler.sendPacket(
            new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0,0,0), dir));
        mc.player.networkHandler.sendPacket(
            new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, blockHitResult, mc.player.currentScreenHandler.getRevision() + 2));
        mc.player.networkHandler.sendPacket(
            new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0,0,0), dir));

        return true;
    }

    private boolean switchToBuildMat() {
        var current = mc.player.getMainHandStack().getItem();
        if(current == Items.SMOOTH_STONE_SLAB) {
            return true;
        }

        var slot = InvUtils.findInHotbar(Items.SMOOTH_STONE_SLAB);
        if(!slot.found()) return false;

        InvUtils.swap(slot.slot(), false);

        // Technically it was successful, but by returning false we delay placement for a tick
        return false;
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        BlockPos[] targets = targets(3);

        if(targets.length == 0) {
            return;
        }

        for(BlockPos block : targets) {
            event.renderer.box(block, innerColor, outerColor, ShapeMode.Both, 0);
        }
    }

    private BlockPos[] targets(int reach) {
        if(mc.player == null || mc.world == null) {
            return new BlockPos[0];
        }

        var pos = mc.player.getPos();
        final int y = (int)Math.round(pos.y);

        if(pos.z < 1533.000 || pos.z > 1534.999) {
            return new BlockPos[0];
        }

        var facing = mc.player.getHorizontalFacing();

        LinkedList<BlockPos> positions = new LinkedList<>();

        for(int i=1; i <= reach; i++) {
            final int offsetX = facing.getOffsetX() * i;
            var b1533 = new BlockPos((int)pos.x + offsetX, y - 1, 1533);
            if(mc.world.getBlockState(b1533).isReplaceable()) {
                positions.add(new BlockPos((int)pos.x + offsetX, y - 1, 1533));
            }

            var b1534 = new BlockPos((int)pos.x + offsetX, y - 1, 1534);
            if(mc.world.getBlockState(b1534).isReplaceable()) {
                positions.add(new BlockPos((int)pos.x + offsetX, y - 1, 1534));
            }
        }

        return positions.toArray(new BlockPos[0]);
    }


    private BlockPos target() {
        if(mc.player == null || mc.world == null) {
            return null;
        }
        var pos = mc.player.getPos();

        var facing = mc.player.getHorizontalFacing();
        pos = pos.add(
            facing.getOffsetX(),
            -1,
            facing.getOffsetZ()
        );





        var block = new BlockPos(
            (int)pos.x,
            (int)Math.round(pos.y),
            (int)pos.z
        );
        return block;
    }

    @Override
    public void onActivate() {
        recentPlacements.clear();
    }
}
