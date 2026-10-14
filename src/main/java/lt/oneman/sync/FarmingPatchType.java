/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package lt.oneman.sync;
import net.runelite.client.plugins.timetracking.farming.CropState;
import net.runelite.client.plugins.timetracking.farming.Produce;
/** Herb/tree decoding adapted from RuneLite PatchImplementation (BSD-2). */
enum FarmingPatchType {
HERB
		{
			@Override
			FarmingPatchDecoder.DecodedPatch decode(int value, long seen)
			{
				if (value >= 0 && value <= 3)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3 - value, seen);
				}
				if (value >= 4 && value <= 7)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GUAM, CropState.GROWING, value - 4, seen);
				}
				if (value >= 8 && value <= 10)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GUAM, CropState.HARVESTABLE, 10 - value, seen);
				}
				if (value >= 11 && value <= 14)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MARRENTILL, CropState.GROWING, value - 11, seen);
				}
				if (value >= 15 && value <= 17)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MARRENTILL, CropState.HARVESTABLE, 17 - value, seen);
				}
				if (value >= 18 && value <= 21)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TARROMIN, CropState.GROWING, value - 18, seen);
				}
				if (value >= 22 && value <= 24)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TARROMIN, CropState.HARVESTABLE, 24 - value, seen);
				}
				if (value >= 25 && value <= 28)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HARRALANDER, CropState.GROWING, value - 25, seen);
				}
				if (value >= 29 && value <= 31)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HARRALANDER, CropState.HARVESTABLE, 31 - value, seen);
				}
				if (value >= 32 && value <= 35)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.RANARR, CropState.GROWING, value - 32, seen);
				}
				if (value >= 36 && value <= 38)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.RANARR, CropState.HARVESTABLE, 38 - value, seen);
				}
				if (value >= 39 && value <= 42)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TOADFLAX, CropState.GROWING, value - 39, seen);
				}
				if (value >= 43 && value <= 45)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TOADFLAX, CropState.HARVESTABLE, 45 - value, seen);
				}
				if (value >= 46 && value <= 49)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.IRIT, CropState.GROWING, value - 46, seen);
				}
				if (value >= 50 && value <= 52)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.IRIT, CropState.HARVESTABLE, 52 - value, seen);
				}
				if (value >= 53 && value <= 56)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.AVANTOE, CropState.GROWING, value - 53, seen);
				}
				if (value >= 57 && value <= 59)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.AVANTOE, CropState.HARVESTABLE, 59 - value, seen);
				}
				if (value >= 60 && value <= 63)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HUASCA, CropState.GROWING, value - 60, seen);
				}
				if (value >= 64 && value <= 66)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HUASCA, CropState.HARVESTABLE, 66 - value, seen);
				}
				if (value == 67)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 68 && value <= 71)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.KWUARM, CropState.GROWING, value - 68, seen);
				}
				if (value >= 72 && value <= 74)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.KWUARM, CropState.HARVESTABLE, 74 - value, seen);
				}
				if (value >= 75 && value <= 78)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.SNAPDRAGON, CropState.GROWING, value - 75, seen);
				}
				if (value >= 79 && value <= 81)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.SNAPDRAGON, CropState.HARVESTABLE, 81 - value, seen);
				}
				if (value >= 82 && value <= 85)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.CADANTINE, CropState.GROWING, value - 82, seen);
				}
				if (value >= 86 && value <= 88)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.CADANTINE, CropState.HARVESTABLE, 88 - value, seen);
				}
				if (value >= 89 && value <= 92)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.LANTADYME, CropState.GROWING, value - 89, seen);
				}
				if (value >= 93 && value <= 95)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.LANTADYME, CropState.HARVESTABLE, 95 - value, seen);
				}
				if (value >= 96 && value <= 99)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.DWARF_WEED, CropState.GROWING, value - 96, seen);
				}
				if (value >= 100 && value <= 102)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.DWARF_WEED, CropState.HARVESTABLE, 102 - value, seen);
				}
				if (value >= 103 && value <= 106)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TORSTOL, CropState.GROWING, value - 103, seen);
				}
				if (value >= 107 && value <= 109)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TORSTOL, CropState.HARVESTABLE, 109 - value, seen);
				}
				if (value >= 128 && value <= 130)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GUAM, CropState.DISEASED, value - 127, seen);
				}
				if (value >= 131 && value <= 133)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MARRENTILL, CropState.DISEASED, value - 130, seen);
				}
				if (value >= 134 && value <= 136)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TARROMIN, CropState.DISEASED, value - 133, seen);
				}
				if (value >= 137 && value <= 139)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HARRALANDER, CropState.DISEASED, value - 136, seen);
				}
				if (value >= 140 && value <= 142)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.RANARR, CropState.DISEASED, value - 139, seen);
				}
				if (value >= 143 && value <= 145)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TOADFLAX, CropState.DISEASED, value - 142, seen);
				}
				if (value >= 146 && value <= 148)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.IRIT, CropState.DISEASED, value - 145, seen);
				}
				if (value >= 149 && value <= 151)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.AVANTOE, CropState.DISEASED, value - 148, seen);
				}
				if (value >= 152 && value <= 154)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.KWUARM, CropState.DISEASED, value - 151, seen);
				}
				if (value >= 155 && value <= 157)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.SNAPDRAGON, CropState.DISEASED, value - 154, seen);
				}
				if (value >= 158 && value <= 160)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.CADANTINE, CropState.DISEASED, value - 157, seen);
				}
				if (value >= 161 && value <= 163)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.LANTADYME, CropState.DISEASED, value - 160, seen);
				}
				if (value >= 164 && value <= 166)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.DWARF_WEED, CropState.DISEASED, value - 163, seen);
				}
				if (value >= 167 && value <= 169)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.TORSTOL, CropState.DISEASED, value - 166, seen);
				}
				if (value >= 170 && value <= 172)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.ANYHERB, CropState.DEAD, value - 169, seen);
				}
				if (value >= 173 && value <= 175)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.HUASCA, CropState.DISEASED, value - 172, seen);
				}
				if (value >= 176 && value <= 191)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 192 && value <= 195)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GOUTWEED, CropState.GROWING, value - 192, seen);
				}
				if (value >= 196 && value <= 197)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GOUTWEED, CropState.HARVESTABLE, 197 - value, seen);
				}
				if (value >= 198 && value <= 200)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GOUTWEED, CropState.DISEASED, value - 197, seen);
				}
				if (value >= 201 && value <= 203)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.GOUTWEED, CropState.DEAD, value - 200, seen);
				}
				if (value >= 204 && value <= 219)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 221 && value <= 255)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				return null;
			}
		},
TREE
		{
			@Override
			FarmingPatchDecoder.DecodedPatch decode(int value, long seen)
			{
				if (value >= 0 && value <= 3)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3 - value, seen);
				}
				if (value >= 4 && value <= 7)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 8 && value <= 11)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.GROWING, value - 8, seen);
				}
				if (value == 12)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.GROWING, Produce.OAK.getStages() - 1, seen);
				}
				if (value == 13)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.HARVESTABLE, 0, seen);
				}
				if (value == 14)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 15 && value <= 20)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.GROWING, value - 15, seen);
				}
				if (value == 21)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.GROWING, Produce.WILLOW.getStages() - 1, seen);
				}
				if (value == 22)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.HARVESTABLE, 0, seen);
				}
				if (value == 23)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 24 && value <= 31)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.GROWING, value - 24, seen);
				}
				if (value == 32)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.GROWING, Produce.MAPLE.getStages() - 1, seen);
				}
				if (value == 33)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.HARVESTABLE, 0, seen);
				}
				if (value == 34)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 35 && value <= 44)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.GROWING, value - 35, seen);
				}
				if (value == 45)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.GROWING, Produce.YEW.getStages() - 1, seen);
				}
				if (value == 46)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.HARVESTABLE, 0, seen);
				}
				if (value == 47)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 48 && value <= 59)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.GROWING, value - 48, seen);
				}
				if (value == 60)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.GROWING, Produce.MAGIC.getStages() - 1, seen);
				}
				if (value == 61)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.HARVESTABLE, 0, seen);
				}
				if (value == 62)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 63 && value <= 72)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 73 && value <= 75)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.DISEASED, value - 72, seen);
				}
				if (value == 77)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.DISEASED, 4, seen);
				}
				if (value >= 78 && value <= 79)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 80 && value <= 84)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.DISEASED, value - 79, seen);
				}
				if (value == 86)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.DISEASED, 6, seen);
				}
				if (value >= 87 && value <= 88)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 89 && value <= 95)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.DISEASED, value - 88, seen);
				}
				if (value == 97)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.DISEASED, 8, seen);
				}
				if (value >= 98 && value <= 99)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 100 && value <= 108)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.DISEASED, value - 99, seen);
				}
				if (value == 110)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.DISEASED, 10, seen);
				}
				if (value >= 111 && value <= 112)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 113 && value <= 123)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.DISEASED, value - 112, seen);
				}
				if (value == 125)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.DISEASED, 12, seen);
				}
				if (value >= 126 && value <= 136)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 137 && value <= 139)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.DEAD, value - 136, seen);
				}
				if (value == 141)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.OAK, CropState.DEAD, 4, seen);
				}
				if (value >= 142 && value <= 143)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 144 && value <= 148)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.DEAD, value - 143, seen);
				}
				if (value == 150)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.DEAD, 6, seen);
				}
				if (value >= 151 && value <= 152)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 153 && value <= 159)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.DEAD, value - 152, seen);
				}
				if (value == 161)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAPLE, CropState.DEAD, 8, seen);
				}
				if (value >= 162 && value <= 163)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 164 && value <= 172)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.DEAD, value - 163, seen);
				}
				if (value == 174)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.YEW, CropState.DEAD, 10, seen);
				}
				if (value >= 175 && value <= 176)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 177 && value <= 187)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.DEAD, value - 176, seen);
				}
				if (value == 189)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.MAGIC, CropState.DEAD, 12, seen);
				}
				if (value >= 190 && value <= 191)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				if (value >= 192 && value <= 197)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WILLOW, CropState.HARVESTABLE, 0, seen);
				}
				if (value >= 198 && value <= 255)
				{
					return new FarmingPatchDecoder.DecodedPatch(Produce.WEEDS, CropState.GROWING, 3, seen);
				}
				return null;
			}
		};
 abstract FarmingPatchDecoder.DecodedPatch decode(int value, long seen);
}
