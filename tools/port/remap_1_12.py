"""Mechanical first pass for porting BuildCraft 1.12.2 sources to Minecraft 1.20.1 (Forge, Mojang mappings).

Renames the classes, imports and common methods whose meaning didn't change between the versions, and lists what is
left for a human to port by hand. The output still needs manual work: it only removes the boring part.

Usage: python -I tools/port/remap_1_12.py <file.java>...   (files are rewritten in place)
       python -I tools/port/remap_1_12.py --sort-imports <file.java>...   (only sorts the imports)
"""

import re
import sys

# 1.12 import -> 1.20 import. The simple class name is renamed too when it changed.
IMPORTS = {
    "net.minecraft.util.EnumFacing": "net.minecraft.core.Direction",
    "net.minecraft.item.EnumDyeColor": "net.minecraft.world.item.DyeColor",
    "net.minecraft.tileentity.TileEntity": "net.minecraft.world.level.block.entity.BlockEntity",
    "net.minecraft.world.World": "net.minecraft.world.level.Level",
    "net.minecraft.world.IBlockAccess": "net.minecraft.world.level.BlockGetter",
    "net.minecraft.util.math.BlockPos": "net.minecraft.core.BlockPos",
    "net.minecraft.util.math.Vec3d": "net.minecraft.world.phys.Vec3",
    "net.minecraft.util.math.Vec3i": "net.minecraft.core.Vec3i",
    "net.minecraft.util.math.AxisAlignedBB": "net.minecraft.world.phys.AABB",
    "net.minecraft.util.math.RayTraceResult": "net.minecraft.world.phys.HitResult",
    "net.minecraft.util.math.MathHelper": "net.minecraft.util.Mth",
    "net.minecraft.nbt.NBTTagCompound": "net.minecraft.nbt.CompoundTag",
    "net.minecraft.nbt.NBTTagList": "net.minecraft.nbt.ListTag",
    "net.minecraft.nbt.NBTBase": "net.minecraft.nbt.Tag",
    "net.minecraft.nbt.NBTTagString": "net.minecraft.nbt.StringTag",
    "net.minecraft.entity.player.EntityPlayer": "net.minecraft.world.entity.player.Player",
    "net.minecraft.entity.player.EntityPlayerMP": "net.minecraft.server.level.ServerPlayer",
    "net.minecraft.entity.item.EntityItem": "net.minecraft.world.entity.item.ItemEntity",
    "net.minecraft.entity.Entity": "net.minecraft.world.entity.Entity",
    "net.minecraft.entity.EntityLivingBase": "net.minecraft.world.entity.LivingEntity",
    "net.minecraft.item.ItemStack": "net.minecraft.world.item.ItemStack",
    "net.minecraft.item.Item": "net.minecraft.world.item.Item",
    "net.minecraft.util.NonNullList": "net.minecraft.core.NonNullList",
    "net.minecraft.network.PacketBuffer": "net.minecraft.network.FriendlyByteBuf",
    "net.minecraft.util.ResourceLocation": "net.minecraft.resources.ResourceLocation",
    "net.minecraft.block.Block": "net.minecraft.world.level.block.Block",
    "net.minecraft.block.state.IBlockState": "net.minecraft.world.level.block.state.BlockState",
    "net.minecraft.util.EnumHand": "net.minecraft.world.InteractionHand",
    "net.minecraft.util.EnumActionResult": "net.minecraft.world.InteractionResult",
    "net.minecraft.util.IStringSerializable": "net.minecraft.util.StringRepresentable",
    "net.minecraft.init.Blocks": "net.minecraft.world.level.block.Blocks",
    "net.minecraft.init.Items": "net.minecraft.world.item.Items",
    "net.minecraftforge.fml.relauncher.Side": "net.minecraftforge.fml.LogicalSide",
    "net.minecraftforge.fluids.Fluid": "net.minecraft.world.level.material.Fluid",
}

# Imports that no longer exist; the code using them has to be ported by hand.
REMOVED_IMPORTS = [
    "net.minecraftforge.fml.relauncher.SideOnly",
    "net.minecraftforge.fml.common.network.simpleimpl.MessageContext",
    "net.minecraftforge.fml.common.network.simpleimpl.IMessage",
    "net.minecraftforge.common.util.Constants",
    "net.minecraftforge.fml.common.API",
]

# Plain identifier renames, applied on word boundaries.
IDENTIFIERS = {
    "EnumFacing": "Direction",
    "EnumDyeColor": "DyeColor",
    "TileEntity": "BlockEntity",
    "World": "Level",
    "IBlockAccess": "BlockGetter",
    "Vec3d": "Vec3",
    "AxisAlignedBB": "AABB",
    "RayTraceResult": "HitResult",
    "MathHelper": "Mth",
    "NBTTagCompound": "CompoundTag",
    "NBTTagList": "ListTag",
    "NBTBase": "Tag",
    "NBTTagString": "StringTag",
    "EntityPlayerMP": "ServerPlayer",
    "EntityPlayer": "Player",
    "EntityItem": "ItemEntity",
    "EntityLivingBase": "LivingEntity",
    "PacketBuffer": "FriendlyByteBuf",
    "IBlockState": "BlockState",
    "EnumHand": "InteractionHand",
    "EnumActionResult": "InteractionResult",
    "IStringSerializable": "StringRepresentable",
    "Side": "LogicalSide",
}

# Method and field renames (regex -> replacement).
MEMBERS = [
    (r"\bDirection\.VALUES\b", "Direction.values()"),
    (r"\.isRemote\b", ".isClientSide"),
    (r"\.getTotalWorldTime\(\)", ".getGameTime()"),
    (r"\.getFrontOffsetX\(\)", ".getStepX()"),
    (r"\.getFrontOffsetY\(\)", ".getStepY()"),
    (r"\.getFrontOffsetZ\(\)", ".getStepZ()"),
    (r"\.getDirectionVec\(\)", ".getNormal()"),
    (r"\.getPos\(\)", ".getBlockPos()"),
    (r"\.getWorld\(\)", ".getLevel()"),
    (r"\.offset\(", ".relative("),
    (r"\.getTagCompound\(\)", ".getTag()"),
    (r"\.setTagCompound\(", ".setTag("),
    (r"\.getCompoundTag\(", ".getCompound("),
    (r"\.getInteger\(", ".getInt("),
    (r"\.setInteger\(", ".putInt("),
    (r"\.setLong\(", ".putLong("),
    (r"\.setBoolean\(", ".putBoolean("),
    (r"\.setDouble\(", ".putDouble("),
    (r"\.setFloat\(", ".putFloat("),
    (r"\.setByte\(", ".putByte("),
    (r"\.setString\(", ".putString("),
    (r"\.setIntArray\(", ".putIntArray("),
    (r"\.setTag\(", ".put("),
    (r"\.hasKey\(", ".contains("),
    (r"\.hasNoTags\(\)", ".isEmpty()"),
    (r"\.splitStack\(", ".split("),
    (r"\.getMetadata\(\)", ".getId()"),
    (r"\.spawnEntity\(", ".addFreshEntity("),
]

# Things that compile in neither version unchanged, listed for the human doing the rest.
TODO_PATTERNS = [
    r"\bSideOnly\b", r"\bMessageContext\b", r"\bIMessage\b", r"Constants\.NBT", r"\bhasCapability\b",
    r"getCapability\([^)]*\)\s*(!=|==)\s*null", r"\bPacketBufferBC\b", r"\bITickable\b", r"\bupdate\(\)",
    r"\breadFromNBT\b", r"\bwriteToNBT\b", r"\bgetTileEntity\b", r"\bmotion[XYZ]\b", r"\bisDead\b",
]


# BuildCraft's import groups, in order. Each group is sorted, and separated from the next by a blank line.
IMPORT_GROUPS = [
    "java.", "javax.", "org.", "com.", "io.", "net.minecraft.", "net.minecraftforge.", "buildcraft.api.",
    "buildcraft.lib.", "buildcraft.",
]


def import_group(line):
    name = line[len("import "):]
    if name.startswith("static "):
        return -1
    for index, prefix in enumerate(IMPORT_GROUPS):
        if name.startswith(prefix):
            return index
    return len(IMPORT_GROUPS)


def sort_imports(text):
    lines = text.split("\n")
    indexes = [i for i, line in enumerate(lines) if line.startswith("import ")]
    if not indexes:
        return text
    start, end = indexes[0], indexes[-1]
    block = [line for line in lines[start:end + 1] if line.strip()]
    if any(not line.startswith("import ") for line in block):
        # Something other than imports (like a comment) is mixed in: leave the block alone
        return text
    groups = {}
    # Sorted without the ";", so that "a.B" comes before "a.B.C"
    for line in sorted(set(block), key=lambda l: l.rstrip(";")):
        groups.setdefault(import_group(line), []).append(line)
    sorted_block = []
    for key in sorted(groups):
        if sorted_block:
            sorted_block.append("")
        sorted_block.extend(groups[key])
    return "\n".join(lines[:start] + sorted_block + lines[end + 1:])


def remap(text):
    for old, new in IMPORTS.items():
        text = text.replace("import " + old + ";", "import " + new + ";")
    for old in REMOVED_IMPORTS:
        text = text.replace("import " + old + ";\n", "")
    # The @API annotation on package-info files is gone
    text = re.sub(r"^@API\(.*\)\n", "", text, flags=re.M)
    for old, new in IDENTIFIERS.items():
        text = re.sub(r"(?<![\w.])" + re.escape(old) + r"\b", new, text)
    for pattern, new in MEMBERS:
        text = re.sub(pattern, new, text)
    return sort_imports(text)


def main(paths):
    transform = remap
    if paths and paths[0] == "--sort-imports":
        transform = sort_imports
        paths = paths[1:]
    for path in paths:
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read().replace("\r\n", "\n")
        text = transform(text)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        for number, line in enumerate(text.splitlines(), 1):
            for pattern in TODO_PATTERNS:
                if re.search(pattern, line):
                    print(f"{path}:{number}: {line.strip()}")
                    break


if __name__ == "__main__":
    main(sys.argv[1:])
