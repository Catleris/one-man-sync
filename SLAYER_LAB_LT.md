# OneMan Slayer Lab 0.3 — testinė kopija

Darbo šaka: `dev/slayer-helper-lab`. Originali `master` šaka ir OneMan Sync pagrindiniai failai nepakeisti.

## Paleidimas / atnaujinimas

1. Uždaryk testinį RuneLite. Naujos versijos kodą naudok atskirame testinio projekto aplanke.
2. Atnaujindamas išsaugok savo `dev-home` aplanką: jame yra testinio kliento prisijungimas, nustatymai ir nauja vietinė istorija. Jo nekelk į GitHub ir nesiųsk kitiems.
3. IntelliJ → Open → projektas. Gradle JVM: JDK 17; Java target: 11.
4. Windows PowerShell: `.\gradlew.bat run`; Linux: `./gradlew run`.
5. Įjunk RuneLite `Slayer` ir `OneMan Slayer Lab`; atidaryk auksinę S piktogramą.

Gradle run naudoja atskirą `user.home=dev-home`. OneMan Sync serverio sinchronizacija pagal nutylėjimą išjungta; Slayer pagalbininkui sync key nereikia.

Jagex paskyros prisijungimas: oficialus RuneLite vadovas
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## Aštuoni nauji rodiniai

- **Prep:** aktyvus taskas, priskirta vieta, Slayer lygis, specialių daiktų patikra. Būsena skiria banke matytą daiktą, inventorių ir užsidėtą įrangą. Atidaryk banką, kad būtų išsaugota jo kopija su data. Pažymėti daiktai ir banko placeholders nepatvirtina pasiruošimo.
- **Tactics:** rankinis monstrų/bossų variantų pasirinkimas, statiniai pasiruošimo ir strategijos patarimai, pasirinkto varianto Wiki. Hydra, Araxxor, Cerberus, Abyssal Sire, Kraken, Grotesque Guardians ir kiti svarbesni variantai turi papildomus patarimus.
- **Places:** įėjimų alternatyvos, prieiga, žinomos cannon/multi sąlygos, banko orientyrai ir Wilderness rizika. Konar priskirtai vietai žymimi tik tiksliai atitinkantys žinomi įėjimai; bendros vietos lieka informacinės.
- **Travel:** kelionės pasirinkimai pagal nešiojamus teleportų daiktus ir patikrintas konkrečių questų baigimo būsenas. Slayer ring, Rada's blessing, Falador tablet, ring of dueling ir royal seed pod rodomi tik su aiškia prieinamumo būsena.
- **Supplies:** maistas, potion dozės, matoma ammo/runes. Plugino nustatymuose pasirink combat style ir savo perspėjimo ribas; nulis išjungia ribą. Desktop alert yra pasirenkamas, pagal nutylėjimą išjungtas.
- **Session:** sesijos laikas, priskirto NPC loot įvykiais užfiksuoti kills/h, atskiras tasko credits/h, Slayer XP/h ir likusio laiko įvertis. Galima išsaugoti sesijos pastabą.
- **Loot:** Ironman upgrade tikslų idėjos su paaiškinimu, pasirenkamas arba įrašomas norimas daiktas, kiekis datuotoje banko kopijoje ir šioje sesijoje stebėtas loot.
- **History:** iki 50 naujausių stebėtų sesijų: taskas, priskirta vieta, pasirinktas įėjimas/variantas, paskutinė įranga, XP, credits, loot ir pastaba. Duomenys atskiri RuneLite paskyros profiliams.

## Aprėptis ir ribos

Įtraukti **158 Wiki ir RuneLite taskų / specialių priskyrimų katalogo įrašai**, tarp jų 95 Slayer Hydras. Tai nėra 158 ranka parašytos išsamios kovos taktikos. Bendras katalogas turi šaltinio lygį, vietas, įrangos/prieigos pastabas ir variantus; svarbesni taskai turi papildomai paruoštą checklistą ar taktikas. Boss/specialiems priskyrimams, kurių bendroje Wiki lentelėje nėra, naudojamas konkretaus monstro Wiki.

Vietų koordinatės yra patikrinti RuneLite įėjimai. Ne visiems įrašams ar kiekvienai jų vietai yra navigacija. **Centre map** centruoja jau atidarytą world map; **Track entrance** įjungia pulsuojantį žymeklį ir minimap kryptį; **Stop tracking** išjungia sekimą. Wilderness įėjimai oranžiniai ir nepasirenkami automatiškai. Tai kryptis į įėjimą, ne pilnas kelio skaičiavimas. Kitoje map area, aukšte ar instance minimap rodyklė nerodoma.

Banko kopija gali pasenti. Questai tikrinami tik aprašytiems kelionės variantams; diary išimtys, fairy-ring dalinis atrakinimas, POH, teleportų dienos limitai, weapon charges, rune pouch ir quiver turinys netikrinami. Karuulm boots checklistas netaikomas kaip draudimas elite diary išimčiai. Checklistas skirtas įprastam tasko monstrui: pasirinkus bossą papildomą setupą būtina patikrinti Tactics / Wiki.

Kills skaičiuojami iš RuneLite priskirtų NPC loot įvykių, todėl ne visi nužudymai gali būti matomi. Tasko credits nėra tikslus kills skaičius dėl apyrankių ir kelių credits. XP apima visą sesijoje gautą Slayer XP. Banking/travel įeina į sesijos laiką. Logout, tasko pakeitimas, išvalymas ar plugino išjungimas archyvuoja sesiją. Ankstesnis žaidimas neatkuriamas. Po nutrūkusio kliento sesija baigiama ties paskutiniu išsaugotu stebėjimu, nepridedant neprisijungto laiko.

Duomenys saugomi vietinėje RuneLite konfigūracijoje, be šio pagalbininko tinklo užklausų. Wiki atidaromas tik paspaudus. Pagalbininkas neatlieka teleportų, ėjimo ar combat veiksmų ir nerodo gyvų boss mechanikų / prayer patarimų.

## Patikrinimas žaidime — atlieka vartotojas

1. Su aktyviu tasku sutikrink pavadinimą, kiekį ir Konar vietą su enchanted gem.
2. Atidaryk banką. Dust devils / Banshees apsaugą perkelk bankas → inventorius → įranga: turi būti TAKE FROM BANK → EQUIP → READY. Black mask nepakeičia facemask; Slayer helmet nepakeičia mirror shield / witchwood icon.
3. Pasirink įėjimą, atidaryk world map → Centre map → Track entrance. Pakeisk tikslą, Stop tracking, patikrink logout ir tasko pasikeitimą. Nežinoma Konar vieta negali rodyti kito požemio.
4. Tactics pasirink bossą ir įprastą monstrą; patikrink besikeičiantį aprašą ir Wiki. Prep yra įprasto monstro, ne automatiškai patvirtintas boss setupas.
5. Travel paimk / padėk charged Slayer ring ar kitą parodytą teleportą; quest apribojimas neturi būti žymimas baigtu neatlikus questo.
6. Supplies pasirink ribas ir combat style; suvalgyk maistą / išgerk potion. Kiekiai ir perspėjimai turi atsinaujinti. Noted supplies neturi skaičiuotis.
7. Session nužudyk kelis tasko monstrus; stebėk credits ir loot kills atskirai. Po 60 s turi atsirasti rates, po bent 3 credits — ETA. Išsaugok pastabą.
8. Loot pasirink tikslą; sutikrink banko kopiją ir stebėtą loot. Logout/login turi išsaugoti History. Kita paskyra neturi matyti pirmos banko ar istorijos.
9. Išjunk/įjunk Lab: neturi dubliuotis S piktograma ar map pins. Originalų OneMan Sync patikrink atskirai — jo pagrindinis kodas nekeistas.

Automatiniai testai tikrina duomenų, kiekių, sesijos, banko, loot ir kelionės logiką. Kompiliavimas ir testai nepakeičia šio žaidimo patikrinimo.

## Duomenų šaltiniai (2026-10-07)

- OSRS Wiki Slayer task lentelė: https://oldschool.runescape.wiki/w/Slayer_task
- Slayer įranga: https://oldschool.runescape.wiki/w/Slayer_equipment
- RuneLite Task ir SlayerConfig: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer
- Įėjimų koordinatės: https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/DungeonLocation.java
- Strategijos: https://oldschool.runescape.wiki/w/Alchemical_Hydra/Strategies , https://oldschool.runescape.wiki/w/Araxxor/Strategies , https://oldschool.runescape.wiki/w/Cerberus/Strategies , https://oldschool.runescape.wiki/w/Abyssal_Sire/Strategies , https://oldschool.runescape.wiki/w/Kraken/Strategies , https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies , https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies
- Kelionės: https://oldschool.runescape.wiki/w/Slayer_ring , https://oldschool.runescape.wiki/w/Rada%27s_blessing , https://oldschool.runescape.wiki/w/Royal_seed_pod

Katalogo faktai išvardyti `slayer-knowledge.tsv`; tai vietinė šaltinių kopija, todėl būsimi žaidimo pakeitimai reikalauja atnaujinimo. Wiki lentelės sąlygos gali priklausyti nuo vietos, varianto ar Slayer master; jos nėra universalūs privalomi reikalavimai.
