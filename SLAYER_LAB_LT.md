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

## Nauja versija 0.2 — žemėlapis ir minimap kryptis

- Žemėlapyje rodomi patikrinti įėjimų taškai 24 taskų tipams (ne visi monstrai ir ne visos jų vietos). Blue dragons: Taverley Dungeon ir Heroes' Guild. Neaprašyti variantai lieka Wiki.
- DESTINATION / ENTRANCE sąraše pasirink vietą. Centre map centruoja jau atidarytą world map į pasirinkimą. Įprasti taškai mėlyni, Wilderness — oranžiniai.
- Track entrance pradeda sekti pasirinktą įėjimą: ryškus mėlynas taškas pulsuoja pasaulio žemėlapyje; minimape yra pulsuojanti krypties rodyklė, o priėjus — įėjimo žymeklis.
- Stop tracking išjungia sekimą. Tasko/vietos pasikeitimas, tasko pabaiga, logout ir plugino išjungimas išvalo jo žymeklius.
- Konar priskirtai vietai rodomos tik tiksliai atitinkančios patikrintos vietos. Jeigu vieta dar neaprašyta, navigacija nepradedama.
- Prieš Track entrance perskaityk prieigos sąlygas. Quest, shortcut ir teleporto atrakinimai šioje versijoje automatiškai netikrinami.

Tai įėjimų/krypties navigacija, o ne pilnas kelių paieškos algoritmas. Atstumas tiesus; mėlyna rodyklė nerodo, kad galima tiesiai pereiti sieną ar kalną. Po įėjimo vadovaukis tasko chamber/floor aprašu; vidinių požemio maršrutų nėra. Kitoje map area, kitame aukšte ar instance minimap rodyklė nesukuriama. Bankas ir kelionės teleportai nepasirenkami automatiškai.

Atnaujinimas: uždaryk Development klientą, pakeisk testinio projekto kodo failus šios versijos failais, išsaugok savo dev-home aplanką ir paleisk `.\gradlew.bat run` iš naujo. Originalaus RuneLite/Plugin Hub failų nekeisk.

Testavimas žaidime:
1. Taskas su keliomis vietomis: pasirink variantą, atidaryk world map, spausk Centre map → Track entrance. Pasirinktas taškas turi pulsuoti.
2. Keliauk link pasirinkto įėjimo: minimap rodyklė turi suktis kartu su kamera, o priėjus pakeisti išvaizdą į įėjimo žymeklį.
3. Pasirink kitą vietą: sekimas pakeičiamas tik paspaudus Track entrance. Stop tracking turi panaikinti rodyklę.
4. Konar taskui neturi būti kitos dungeon vietos. Neaprašytai priskirtai vietai turi būti išjungti navigacijos mygtukai.
5. Po logout / task change / plugino išjungimo neturi likti žymeklių. Patikrink fixed ir resizable minimap.

Koordinačių šaltinis: RuneLite DungeonLocation (įėjimai):
https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/DungeonLocation.java

Jagex login: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
