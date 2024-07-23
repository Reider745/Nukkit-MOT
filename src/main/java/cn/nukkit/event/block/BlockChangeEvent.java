package cn.nukkit.event.block;

import cn.nukkit.block.Block;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;

public class BlockChangeEvent extends BlockEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Block blockPrevious;
    /**
     * Generic block event.
     * NOTICE: This event isn't meant to be called.
     *
     * @param block Block.
     */
    public BlockChangeEvent(Block block, Block blockPrevious) {
        super(block);

        this.blockPrevious = blockPrevious;
    }

    public Block getBlockPrevious() {
        return blockPrevious;
    }
}
