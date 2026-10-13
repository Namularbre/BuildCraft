/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.tiles;

/** This interface should be implemented by machines that can tell whether they have work to do. It is exposed with
 * {@link TilesAPI#CAP_HAS_WORK}, and is what the "work scheduled" and "work done" gate triggers check. */
public interface IHasWork {
    /** Check if the block entity is currently doing any work.
     *
     * @return True if the block entity is doing work. */
    boolean hasWork();
}
