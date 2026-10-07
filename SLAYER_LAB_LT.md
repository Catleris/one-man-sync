# OneMan Slayer Lab — testinė kopija

Bazė: Catleris/one-man-sync, master a0166081ff023027768f8ac2f0c51e64c710b21d.
Darbo šaka: dev/slayer-helper-lab. Originalūs OneManSyncPlugin.java ir OneManSyncConfig.java nepakeisti.

## Paleidimas Windows / IntelliJ

1. Parsisiųsk šios šakos ZIP arba klonuok ją į naują aplanką. Neišpakuok ant seno plugino.
2. IntelliJ → Open → pasirink naują aplanką. Gradle JVM: JDK 17; kodas kompiliuojamas Java 11 target.
3. Gradle → Tasks → other → run, arba PowerShell naujame aplanke: `.\gradlew.bat run`.
4. Development RuneLite įjunk `Slayer` ir `OneMan Slayer Lab`. Dešinėje paspausk auksinę S piktogramą.
5. `OneMan Sync` yra nukopijuotas, tačiau sinchronizacija pagal nutylėjimą išjungta. Slayer pagalbininkui sync key nereikia.

Gradle run naudoja atskirą user.home: projekto dev-home. Todėl įprastos RuneLite paskyros nustatymai ir sync key neperimami. Šio aplanko nekelk į GitHub ir nesiųsk kitiems. Neįjunk OneMan Sync serverio sinchronizacijos, jeigu testuoji tik vietinį Slayer skydelį.

Jagex paskyros prisijungimas prie development kliento: laikykis oficialaus RuneLite vadovo:
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
Nesiųsk prisijungimo duomenų ar JX_* reikšmių kitiems.

## Ką rodo

- Dabartinį taską ir likusį kiekį; duomenys iš RuneLite Slayer profilio ir žaidimo task count, atnaujinami maždaug kas 3 sekundes.
- 25 paruoštus taskų aprašus, monstrų vietas, kelionės ir prieigos sąlygas.
- Būtinų specialių daiktų patikrą užrašu READY, EQUIP arba MISSING. Inventoriuje esantis apsaugos daiktas nėra laikomas užsidėtu; pažymėti (noted) daiktai netinka.
- Slayer helmet alternatyvas tik ten, kur jis tikrai pakeičia apsaugą. Jis nepakeičia mirror shield ar witchwood icon.
- Priskirtą vietą (pvz., Konar) atskirai; kitos vietos tokio tasko atveju yra tik informacinės.
- Neaprašytam taskui rodo Wiki mygtuką ir aiškiai nekuria spėjamų reikalavimų.

Bankas, quest completion, potion charges ir pakankami consumable kiekiai netikrinami. Įrangos patikra patvirtina tik daikto turėjimą/užsidėjimą. Blue dragons apsauga ir Turoth/Kurask visas combat setup aprašomi tekstu, automatiškai jų paruošimas nepatvirtinamas. Vietų aprašai nėra automatinė navigacija.

## Patikrinimas žaidime (atlieka vartotojas)

1. Prisijunk turėdamas taską → tikrink pavadinimą, kiekį ir vietą su enchanted gem / Slayer master.
2. Pakeisk taską → naujas pavadinimas turi atsirasti be kliento restarto.
3. Dust devils/Banshees: apsaugą padėk banke, paimk į inventorių, užsidėk. Būsena turi keistis MISSING → EQUIP → READY.
4. Patikrink spalvotą/imbued Slayer helmet. Black mask neturi pakeisti facemask/earmuffs apsaugos.
5. Basilisk/Cockatrice: Slayer helmet neturi patvirtinti mirror shield. Cave horror: neturi pakeisti witchwood icon.
6. Gargoyles: rock hammer inventoriuje arba granite hammer turi būti atpažįstamas.
7. Konar taskas: priskirta vieta išlieka virš bendrų vietų ir negali būti tyliai pakeista kita.
8. Užbaigęs / atšaukęs taską → nėra aktyvaus tasko. Atsijungęs → prisijungimo pranešimas; kita paskyra neturi matyti ankstesnio tasko.
9. Išjunk / įjunk Lab pluginą → nėra dubliuotos S piktogramos.

## Šaltiniai

- RuneLite SlayerPlugin / SlayerConfig (task detection): https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer
- OSRS Wiki: https://oldschool.runescape.wiki/w/Slayer_equipment
- Taskų vietos ir individualūs reikalavimai: https://oldschool.runescape.wiki/w/Slayer_task
- Blue dragon kelias: https://oldschool.runescape.wiki/w/Blue_dragon
- Gargoyle įrankiai: https://oldschool.runescape.wiki/w/Gargoyle

Ne visi taskai turi išsamų aprašą; Wiki mygtukas skirtas naujoms ir neaprašytoms užduotims patikrinti.
