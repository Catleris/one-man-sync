# OneMan Sync 0.6 — sujungtas Slayer / Prayer

Darbo šaka: `dev/slayer-helper-lab`. Originali `master` šaka ir OneMan Sync pagrindiniai failai nepakeisti.

## Paleidimas / atnaujinimas

1. Uždaryk testinį RuneLite. Naujos versijos kodą naudok atskirame testinio projekto aplanke.
2. Atnaujindamas išsaugok savo `dev-home` aplanką: jame yra testinio kliento prisijungimas, nustatymai ir nauja vietinė istorija. Jo nekelk į GitHub ir nesiųsk kitiems.
3. IntelliJ → Open → projektas. Gradle JVM: JDK 17; Java target: 11.
4. Windows PowerShell: `.\gradlew.bat run`; Linux: `./gradlew run`.
5. Įjunk RuneLite `OneMan Sync` (RuneLite `Slayer` priklausomybė įjungiama kartu); atidaryk auksinę S piktogramą.

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

## Nauja 0.4 — Prayer resource monitor

Devyni skirtukai; naujas **Prayer** veikia ir be Slayer tasko. Įjunk `OneMan Sync` ir atidaryk S → Prayer. Rodomi dabartiniai Prayer taškai, tik jau įjungti prayers, nešiojamų Prayer potion / Super restore (įskaitant mixes) dozės ir apytikslis išsekimo laikas. NPC atakos ar projectiles neskaitomi; jokio Prayer pasirinkimo ar keitimo indikatoriaus nėra.

Plugino nustatymai:
- **Prayer points warning below**: pradinis limitas 15; perspėja kai taškų mažiau, ne kai lygiai 15.
- **Prayer / restore doses below**: pradinis limitas 2; combined doses, ne potion buteliukų skaičius.
- **Desktop Prayer alerts**: pagal nutylėjimą išjungta; įjunk, jeigu nori RuneLite pranešimų. Perspėjimas vieną kartą per žemos būsenos epizodą; atsikuria taškus / papildžius dozes persijungia naujam epizodui. Jų matomumas/garsas priklauso nuo RuneLite notification nustatymų.
- Nulis išjungia atitinkamą ribą. Vaizdiniai perspėjimai rodomi Prayer tekste ir spalvotame skirtuko pavadinime.

Įvertis remiasi iki 30 s stebėtų taškų nuostoliais pagal game ticks; reikia bent 12 s stabilaus prayer rinkinio ir bent 2 prarastų taškų. Pakeitus prayers, įrangą ar padidėjus taškams, duomenys renkami iš naujo. Flicking, išoriniai taškų nuostoliai, regeneration ir lag gali iškreipti įvertį. Tai nėra tiksli boss atakų ar Prayer bonus formulė. Sanfew serum, blighted potions, bankas ir kiti atkūrimo šaltiniai nepriskaičiuojami. Potion dozės neatstoja vienodo atkuriamų taškų kiekio.

Papildomas žaidimo testas:
1. Be Slayer tasko atidaryk Prayer skirtuką — turi rodyti taškus ir jau aktyvius prayers.
2. Įjunk/išjunk pasirinktą prayer pats; sąrašas turi atsinaujinti kitame game tick.
3. Perženk taškų ribą: turi atsirasti vizualus perspėjimas. Įjungus desktop alerts, žemo epizodo metu neturi kartotis pranešimai kas tick.
4. Paimk 4-dose Prayer potion ir 3-dose Super restore: kiekiai 4 ir 3. Išgerk dozę; noted items/banko kopija nedidina šio kiekio.
5. Su stabiliu naudojimu palauk bent 12 s / 2 prarastus taškus; turi atsirasti apytikslis laikas. Išgerk potion ar pakeisk prayers/įrangą — įvertis turi pradėti rinkti duomenis iš naujo.
6. Logout / kita paskyra / plugin restart neturi perkelti ankstesnio įverčio ar perspėjimo epizodo.

Atnaujinant vėl išsaugok `dev-home`, įskaitant ten esančią `.runelite` konfigūraciją ir savo vietinį Jagex prisijungimo failą. Credentials failo niekam nesiųsk ir nekelk į GitHub.

## 0.5 — OneMan Sync Slayer pataisa

Šioje testinėje šakoje atnaujintas ir nukopijuotas **OneMan Sync** siuntimo kodas. `master` šaka nepakeista. Slayer Lab pats į oneman.lt nesiunčia. Norint atnaujinti svetainės duomenis, Development kliente atskirai įjunk **OneMan Sync → Enable sync**, įrašyk savo OneMan svetainėje sugeneruotą **Sync Key**. Jo niekam nesiųsk. Preserve `dev-home`; jo nekelk į GitHub.

Siunčiama naujai perskaityta tasko būsena, normalus task streak ir named rewards. Gyvas count 0 nebepakeičiamas senu RuneLite profilio count; tasko pavadinimas neišsaugomas neribotai ar kitam target. Prarastas patvirtinimas aiškiai pažymimas. Atrakintų rewards pokyčiai inicijuoja sync. Praėjus login sync / nustačius taską sutikrink svetainės Slayer timestamp; neįrašome 194 ar kitų vartotojo pasakytų skaičių rankiniu būdu.

Serverio ir Account Hub rodinio pataisa turi būti įdiegta svetainėje atskirai. Iki to senas UI gali vis tiek rodyti statinę Bigger and Badder rekomendaciją. Plugin Hub šio darbo metu dar nurodo b6b7696; jo atnaujinimas nepriklauso nuo vietinio Developer kliento.

## 0.6 — vienas įskiepis

Slayer Lab sujungtas į OneMan Sync: vienas įjungimas, bendri nustatymai ir S skydelis su visais devyniais skirtukais. Slayer ir Prayer veikia net išjungus `Enable OneMan Sync`. Svetainės siuntimui vis dar reikia įjungti šį nustatymą ir įrašyti Sync Key. Ankstesni Lab perspėjimų nustatymai perkeliami vieną kartą, neperrašant jau įrašytų Sync nustatymų. Banko duomenys, istorija ir tikslai išsaugomi pagal tą patį RuneScape profilį.

Atnaujinant nekeisk ir netrink `dev-home`: jame yra tavo testinės aplinkos prisijungimas, nustatymai ir istorija. Naujas paleidiklis krauna tik OneMan Sync, seno atskiro Lab įjungti nereikia. IntelliJ Gradle → Tasks → other → run arba PowerShell projekto kataloge `./gradlew.bat run`.

Patikrink žaidime: vienas OneMan įskiepis; S skirtukai; tasko pasikeitimas ir likutis; žemėlapio vietos; Prayer perspėjimai; ankstesnė istorija po perkrovimo; vietinis skydelis išjungus svetainės sync. Su įjungtu sync patikrink atsinaujinantį taską svetainėje (svetainės serverio pataisa platinama atskirai).
