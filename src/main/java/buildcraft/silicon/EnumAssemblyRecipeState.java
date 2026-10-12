/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

/** What the player chose to do with a recipe of an assembly table. */
public enum EnumAssemblyRecipeState {
    /** The items are there, but the player hasn't asked for it. */
    POSSIBLE,
    /** Asked for, but some items are missing. */
    SAVED,
    /** Asked for, with every item: it will be made in turn. */
    SAVED_ENOUGH,
    /** Being made. */
    SAVED_ENOUGH_ACTIVE
}
