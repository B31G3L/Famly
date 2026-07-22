package com.beigel.famly.data.repository

import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.data.model.FamilyMember
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.MemberStatus
import com.beigel.famly.data.model.Person
import com.beigel.famly.data.model.TreePosition

interface FamilyRepository {
    fun getCurrentUserName(): String
    fun getFamilyTree(): FamilyTree
    fun getRecentlyAdded(): List<Person>
    fun getTreeMembers(): List<Person>
    fun getPersonById(id: String): Person?
    fun getFamilyMembers(): List<FamilyMember>
    fun getInviteCode(): String
}

class FakeFamilyRepository : FamilyRepository {

    private val oma = Person(
        id = "oma",
        name = "Oma Grete",
        initial = "O",
        relation = "Großmutter",
        accent = AvatarAccent.YELLOW,
        birthDate = "3. Mai 1945",
        birthPlace = "Stuttgart",
        bio = "Grete führte über 40 Jahre lang die Familiengärtnerei und liebt es, samstags zu backen.",
        treePosition = TreePosition(0, 0)
    )

    private val opa = Person(
        id = "opa",
        name = "Opa Heinz",
        initial = "O",
        relation = "Großvater",
        accent = AvatarAccent.YELLOW,
        birthDate = "17. Januar 1943",
        birthPlace = "Ulm",
        bio = "Heinz war Schreiner und baut heute noch kleine Holzspielzeuge für die Enkel.",
        treePosition = TreePosition(0, 1)
    )

    private val tanteErika = Person(
        id = "tante_erika",
        name = "Tante Erika",
        initial = "E",
        relation = "Tante",
        accent = AvatarAccent.GREEN,
        birthDate = "9. Juni 1970",
        birthPlace = "Ulm",
        bio = "Erika lebt mit ihrer Familie in Freiburg und organisiert jedes Jahr das Sommerfest.",
        treePosition = TreePosition(1, 0)
    )

    private val mama = Person(
        id = "mama",
        name = "Mama",
        initial = "M",
        relation = "Mutter",
        accent = AvatarAccent.PETROL,
        birthDate = "22. April 1968",
        birthPlace = "Stuttgart",
        bio = "Mama arbeitet als Krankenschwester und liebt lange Spaziergänge im Wald.",
        treePosition = TreePosition(1, 1)
    )

    private val papa = Person(
        id = "papa",
        name = "Papa",
        initial = "P",
        relation = "Vater",
        accent = AvatarAccent.PETROL,
        birthDate = "11. Februar 1966",
        birthPlace = "Ulm",
        bio = "Papa ist begeisterter Hobbykoch und probiert jeden Sonntag ein neues Rezept.",
        treePosition = TreePosition(1, 2)
    )

    private val bruderTom = Person(
        id = "bruder_tom",
        name = "Bruder Tom",
        initial = "T",
        relation = "Bruder",
        accent = AvatarAccent.ORANGE,
        birthDate = "30. September 1995",
        birthPlace = "München",
        bio = "Tom studiert Maschinenbau und spielt in seiner Freizeit Gitarre in einer Band.",
        treePosition = TreePosition(2, 0)
    )

    private val ich = Person(
        id = "ich",
        name = "Ich",
        initial = "I",
        relation = "Ich",
        accent = AvatarAccent.PETROL,
        treePosition = TreePosition(2, 1)
    )

    private val lena = Person(
        id = "lena",
        name = "Lena Müller",
        initial = "L",
        relation = "Schwester",
        accent = AvatarAccent.ORANGE,
        birthDate = "12. März 1998",
        birthPlace = "München",
        isDeceased = false,
        bio = "Lena liebt Fotografie und lebt seit 2020 in München. Sie besucht die Familie jedes Weihnachten.",
        connections = listOf("Mama", "Papa"),
        treePosition = TreePosition(2, 2)
    )

    private val allMembers = listOf(oma, opa, tanteErika, mama, papa, bruderTom, ich, lena)

    override fun getCurrentUserName(): String = "Anna"

    override fun getFamilyTree(): FamilyTree = FamilyTree(
        id = "familie_mueller",
        name = "Familie Müller",
        memberCount = 12,
        members = allMembers
    )

    override fun getRecentlyAdded(): List<Person> = listOf(oma, lena)

    override fun getTreeMembers(): List<Person> = allMembers

    override fun getPersonById(id: String): Person? = allMembers.find { it.id == id }

    override fun getFamilyMembers(): List<FamilyMember> = listOf(
        FamilyMember(ich.copy(name = "Ich"), "Besitzer", MemberStatus.OWNER),
        FamilyMember(lena, "Mitglied", MemberStatus.PENDING)
    )

    override fun getInviteCode(): String = "OFFSHOOT-7F3K2"
}
