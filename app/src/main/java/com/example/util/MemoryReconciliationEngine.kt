package com.example.util

import com.example.data.model.DailyDebriefResult
import com.example.data.model.DebriefNoteUpdate
import com.example.data.model.DebriefPersonUpdate
import com.example.data.model.NoteWithDetails
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Stateful Memory Reconciliation Engine ("Living Notes").
 *
 * Ensures that when new information arrives (via Daily Debrief voice/text, Quick Capture / Note Editor,
 * or Ask AI Concierge) that relates to and modifies an existing note—such as paying down a debt
 * ("I owe Jason $600" -> "I paid Jason $200" => "$400"), adjusting a budget, rescheduling a meeting,
 * or updating a project/person status—the existing note and linked person facts are updated in-place
 * to reflect the current truth alongside an audit trail.
 */
object MemoryReconciliationEngine {

    private val numberFormatter = DecimalFormat("#,##0.##")
    private val plainNumberFormatter = DecimalFormat("0.##")

    private val debtCreationKeywords = setOf(
        "owe", "owes", "owed", "owing", "lent", "loan", "loaned", "borrowed", "borrow",
        "debt", "balance", "tab", "unpaid", "due", "budget", "remaining", "left"
    )

    private val paymentDeltaKeywords = setOf(
        "paid", "pay", "paying", "repaid", "repay", "returned", "gave", "sent", "transferred",
        "venmoed", "zelled", "cashapped", "deduct", "deducted", "subtract", "subtracted",
        "spent", "used", "knocked off", "took off", "taken off", "settled", "cleared"
    )

    private val additionDeltaKeywords = setOf(
        "borrowed another", "borrowed more", "lent another", "lent more",
        "added", "increased by", "plus another", "owes another", "owe another"
    )

    private val statusUpdateKeywords = setOf(
        "update", "updated", "change", "changed", "moved", "rescheduled", "postponed",
        "canceled", "cancelled", "finished", "completed", "done", "resolved", "actually",
        "instead", "no longer", "now"
    )

    private val commonStopWords = setOf(
        "the", "and", "for", "with", "that", "this", "from", "have", "has", "had",
        "was", "were", "are", "is", "be", "been", "to", "in", "on", "at", "of", "by",
        "about", "into", "today", "yesterday", "tomorrow", "just", "note", "memory",
        "said", "told", "talked", "spoke", "met", "also", "very", "really", "dollar", "dollars",
        "saw", "seen", "see", "went", "got", "get", "did", "done", "make", "made", "take", "took"
    )

    /**
     * Comprehensive blocklist of English verbs, auxiliaries, pronouns, prepositions,
     * time words, and common nouns that must NEVER be created as People/Contact profiles.
     */
    private val invalidPersonWords = setOf(
        // Auxiliaries, modals & common verbs (including "have", "saw", "been")
        "have", "has", "had", "having", "haven", "hadn", "hasn",
        "be", "am", "is", "are", "was", "were", "been", "being",
        "see", "saw", "seen", "seeing", "sees",
        "do", "does", "did", "done", "doing", "didn", "doesn", "don",
        "go", "goes", "went", "gone", "going",
        "get", "gets", "got", "gotten", "getting",
        "make", "makes", "made", "making",
        "take", "takes", "took", "taken", "taking",
        "give", "gives", "gave", "given", "giving",
        "come", "comes", "came", "coming",
        "know", "knows", "knew", "known", "knowing",
        "think", "thinks", "thought", "thinking",
        "say", "says", "said", "saying",
        "tell", "tells", "told", "telling",
        "call", "calls", "called", "calling",
        "ask", "asks", "asked", "asking",
        "meet", "meets", "met", "meeting", "meetings",
        "send", "sends", "sent", "sending",
        "pay", "pays", "paid", "paying", "repaid", "repay",
        "owe", "owes", "owed", "owing",
        "lend", "lends", "lent", "lending", "loan", "loaned", "loans",
        "borrow", "borrows", "borrowed", "borrowing",
        "buy", "buys", "bought", "buying",
        "bring", "brings", "brought", "bringing",
        "need", "needs", "needed", "needing",
        "want", "wants", "wanted", "wanting",
        "use", "uses", "used", "using",
        "find", "finds", "found", "finding",
        "put", "puts", "putting",
        "mean", "means", "meant", "meaning",
        "keep", "keeps", "kept", "keeping",
        "let", "lets", "letting",
        "begin", "begins", "began", "begun", "beginning",
        "seem", "seems", "seemed",
        "help", "helps", "helped", "helping",
        "talk", "talks", "talked", "talking",
        "speak", "speaks", "spoke", "spoken", "speaking",
        "turn", "turns", "turned", "turning",
        "start", "starts", "started", "starting",
        "show", "shows", "showed", "shown", "showing",
        "hear", "hears", "heard", "hearing",
        "play", "plays", "played", "playing",
        "run", "runs", "ran", "running",
        "move", "moves", "moved", "moving",
        "like", "likes", "liked", "liking",
        "live", "lives", "lived", "living",
        "believe", "believes", "believed",
        "hold", "holds", "held", "holding",
        "happen", "happens", "happened",
        "write", "writes", "wrote", "written", "writing",
        "provide", "provides", "provided",
        "sit", "sits", "sat", "sitting",
        "stand", "stands", "stood", "standing",
        "lose", "loses", "lost", "losing",
        "add", "adds", "added", "adding",
        "change", "changes", "changed", "changing",
        "follow", "follows", "followed", "following",
        "stop", "stops", "stopped", "stopping",
        "create", "creates", "created", "creating",
        "read", "reads", "reading",
        "allow", "allows", "allowed",
        "spend", "spends", "spent", "spending",
        "grow", "grows", "grew", "grown",
        "open", "opens", "opened", "opening",
        "walk", "walks", "walked", "walking",
        "win", "wins", "won", "winning",
        "offer", "offers", "offered",
        "remember", "remembers", "remembered",
        "love", "loves", "loved",
        "consider", "considers", "considered",
        "appear", "appears", "appeared",
        "wait", "waits", "waited", "waiting",
        "serve", "serves", "served",
        "expect", "expects", "expected",
        "build", "builds", "built", "building",
        "stay", "stays", "stayed", "staying",
        "fall", "falls", "fell", "fallen",
        "cut", "cuts", "cutting",
        "reach", "reaches", "reached",
        "remain", "remains", "remained",
        "suggest", "suggests", "suggested",
        "raise", "raises", "raised",
        "pass", "passes", "passed",
        "sell", "sells", "sold", "selling",
        "require", "requires", "required",
        "report", "reports", "reported",
        "decide", "decides", "decided",
        "pull", "pulls", "pulled",
        "return", "returns", "returned",
        "explain", "explains", "explained",
        "hope", "hopes", "hoped",
        "develop", "develops", "developed",
        "carry", "carries", "carried",
        "break", "breaks", "broke", "broken",
        "receive", "receives", "received",
        "agree", "agrees", "agreed",
        "support", "supports", "supported",
        "hit", "hits", "hitting",
        "produce", "produces", "produced",
        "eat", "eats", "ate", "eaten", "eating",
        "cover", "covers", "covered",
        "catch", "catches", "caught",
        "draw", "draws", "drew", "drawn",
        "choose", "chooses", "chose", "chosen",
        "check", "checked", "checking",
        "finish", "finished", "complete", "completed",
        "schedule", "scheduled", "reschedule", "rescheduled",
        "cancel", "canceled", "cancelled",
        "update", "updated", "updates",
        "feel", "feels", "felt", "feeling",
        "try", "tries", "tried", "trying",
        "leave", "leaves", "left", "leaving",
        "work", "works", "worked", "working",
        "look", "looks", "looked", "looking",
        "will", "would", "shall", "should", "can", "could", "may", "might", "must",
        // Pronouns, determiners, prepositions, conjunctions & adverbs
        "i", "me", "my", "mine", "myself", "you", "your", "yours", "yourself",
        "he", "him", "his", "himself", "she", "her", "hers", "herself",
        "it", "its", "itself", "we", "us", "our", "ours", "ourselves",
        "they", "them", "their", "theirs", "themselves",
        "this", "that", "these", "those", "who", "whom", "whose", "which", "what",
        "where", "when", "why", "how", "someone", "somebody", "anyone", "anybody",
        "everyone", "everybody", "no", "nobody", "nothing", "something", "anything", "everything",
        "the", "a", "an", "some", "any", "all", "both", "each", "every", "either", "neither",
        "another", "other", "others", "such", "many", "much", "more", "most", "few", "less", "least",
        "own", "same", "so", "than", "too", "very", "just", "now", "then", "here", "there",
        "always", "never", "often", "sometimes", "already", "still", "yet", "also", "only", "even",
        "almost", "nearly", "really", "quite", "rather", "pretty", "well", "back", "away", "together",
        "to", "from", "in", "on", "at", "by", "for", "with", "about", "against", "between", "into",
        "through", "during", "before", "after", "above", "below", "up", "down", "out", "off", "over", "under",
        "again", "further", "once", "around", "near", "since", "until", "while", "of", "as", "if", "or",
        "and", "but", "nor", "because", "although", "though", "unless", "not",
        // Time, dates, common nouns & app domain terms
        "today", "yesterday", "tomorrow", "tonight", "morning", "afternoon", "evening", "night",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "january", "february", "march", "april", "june", "july", "august", "september", "october", "november", "december",
        "time", "times", "day", "days", "week", "weeks", "month", "months", "year", "years", "weekend",
        "note", "notes", "memory", "memories", "reminder", "reminders", "task", "tasks", "todo", "todos",
        "action", "actions", "item", "items", "project", "projects", "place", "places", "location", "locations",
        "idea", "ideas", "thoughts", "summary", "debrief", "journal", "entry", "list", "lists",
        "home", "house", "office", "store", "shop", "market", "grocery", "groceries", "gym", "school", "class",
        "hospital", "clinic", "doctor", "dentist", "bank", "account", "church", "park", "airport", "station",
        "hotel", "restaurant", "cafe", "coffee", "lunch", "dinner", "breakfast", "brunch", "food", "drink", "drinks",
        "money", "cash", "dollar", "dollars", "buck", "bucks", "usd", "cent", "cents", "price", "cost", "bill", "bills",
        "rent", "tax", "taxes", "debt", "budget", "balance", "payment", "invoice", "receipt", "card", "credit",
        "phone", "email", "text", "message", "voicemail", "video", "photo", "picture", "image", "file", "document", "book",
        "car", "truck", "bus", "train", "flight", "plane", "trip", "vacation", "holiday", "weather",
        "person", "people", "contact", "contacts", "friend", "friends", "family", "client", "clients", "colleague", "colleagues",
        "team", "group", "company", "business", "manager", "boss", "partner", "kid", "kids", "child", "children",
        "dog", "cat", "pet", "thing", "things", "stuff", "lot", "bit", "way", "ways", "part", "parts", "side", "kind", "sort",
        "type", "types", "info", "information", "data", "detail", "details", "point", "points", "issue", "issues", "problem",
        "question", "answer", "result", "reason", "plan", "plans", "goal", "goals", "habit", "habits", "health", "workout",
        "general", "personal", "finance", "travel", "learning", "shopping", "untitled", "unknown", "none", "null"
    )

    /**
     * Validates whether a candidate string is a genuine human proper name and NOT a verb,
     * pronoun, preposition, or common noun (like "Have", "Saw", "Been", "Notes", "Meeting").
     */
    fun isValidPersonName(
        candidate: String,
        knownPeopleNames: Collection<String> = emptyList()
    ): Boolean {
        val cleaned = candidate.trim().trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']')
        if (cleaned.length !in 2..40) return false

        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty() || tokens.size > 3) return false

        // Reject immediately if ANY word is in invalidPersonWords (even if it was previously in knownPeopleNames)
        for (token in tokens) {
            val lower = token.lowercase()
            if (lower in invalidPersonWords) return false
            if (lower.length < 2) return false
            if (!token.matches(Regex("^[A-Za-z][A-Za-z'\\-]{1,20}$"))) return false
        }

        // If it matches a known valid person, accept
        if (knownPeopleNames.any { it.trim().equals(cleaned, ignoreCase = true) }) {
            return true
        }

        return true
    }

    /**
     * Normalizes note body content for duplicate detection across notes with different titles/headings.
     */
    fun normalizeNoteContentForDedup(content: String): String {
        return content
            .lines()
            .filterNot { line ->
                val t = line.trim()
                t.startsWith("🔄 Updated") || t.startsWith("📝 Update History")
            }
            .joinToString(" ")
            .lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Returns true if two note bodies have the exact same normalized main content.
     */
    fun areNoteContentsEquivalent(contentA: String, contentB: String): Boolean {
        val normA = normalizeNoteContentForDedup(contentA)
        val normB = normalizeNoteContentForDedup(contentB)
        return normA.isNotBlank() && normA == normB
    }

    data class MonetaryExtraction(
        val amount: Double,
        val rawMatch: String,
        val hasDollarSign: Boolean,
        val hasDollarsWord: Boolean
    )

    /**
     * Extracts all monetary or relevant numeric amounts from text.
     * Supports "$600", "$600.50", "600 dollars", "600 bucks", "USD 600", or standalone numbers
     * in financial contexts.
     */
    fun extractMonetaryAmounts(text: String): List<MonetaryExtraction> {
        val results = mutableListOf<MonetaryExtraction>()
        val cleanText = text.replace(Regex("🔄\\s*Updated.*"), "") // Ignore historical log lines when extracting current primary balance

        // 1. Explicit currency matches: $600, $1,200.50, 600 dollars, 600 bucks, 600 usd
        val explicitRegex = Regex(
            "(?:\\$\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?))|(?:\\b([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(dollars?|bucks?|usd)\\b)",
            RegexOption.IGNORE_CASE
        )

        explicitRegex.findAll(cleanText).forEach { match ->
            val dollarGroup = match.groups[1]?.value
            val wordGroup = match.groups[2]?.value
            val numStr = (dollarGroup ?: wordGroup)?.replace(",", "") ?: return@forEach
            val value = numStr.toDoubleOrNull() ?: return@forEach
            results.add(
                MonetaryExtraction(
                    amount = value,
                    rawMatch = match.value.trim(),
                    hasDollarSign = dollarGroup != null,
                    hasDollarsWord = wordGroup != null
                )
            )
        }

        if (results.isNotEmpty()) return results

        // 2. If the text has financial keywords (owe, paid, lent, borrowed, debt, balance), match standalone numbers
        val lower = cleanText.lowercase()
        val isFinancialContext = debtCreationKeywords.any { lower.contains(it) } ||
                paymentDeltaKeywords.any { lower.contains(it) }
        if (isFinancialContext) {
            val numberRegex = Regex("\\b([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{2,6}(?:\\.[0-9]{1,2})?)\\b")
            numberRegex.findAll(cleanText).forEach { match ->
                val raw = match.groupValues[1].replace(",", "")
                val value = raw.toDoubleOrNull() ?: return@forEach
                // Avoid matching years like 2024, 2025, 2026 unless preceded by financial verb
                val beforeIdx = (match.range.first - 15).coerceAtLeast(0)
                val prefix = cleanText.substring(beforeIdx, match.range.first).lowercase()
                val looksLikeYear = value in 2020.0..2035.0 && (prefix.contains("in ") || prefix.contains("on ") || prefix.contains("/"))
                if (!looksLikeYear) {
                    results.add(
                        MonetaryExtraction(
                            amount = value,
                            rawMatch = match.value.trim(),
                            hasDollarSign = false,
                            hasDollarsWord = false
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Extracts validated person names from text without ever matching verbs, pronouns, or common nouns
     * (like "have", "saw", "been", "meeting", "notes").
     */
    fun extractCandidateNames(text: String, knownPeopleNames: List<String> = emptyList()): List<String> {
        val validKnownNames = knownPeopleNames.filter { isValidPersonName(it) }
        val found = linkedSetOf<String>()

        // 1. Check known people first (case-insensitive)
        for (known in validKnownNames) {
            val trimmed = known.trim()
            if (trimmed.isNotBlank() && Regex("\\b${Regex.escape(trimmed)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                found.add(trimmed)
            }
        }

        // 2. Explicit financial verb patterns (supports lowercase names like "I owe jason 600 dollars" or "paid jason $200")
        val financialPersonRegex = Regex(
            "\\b(?:owe|owes|owed|paid|pay|repaid|lent|loaned|borrowed\\s+from)\\s+([a-zA-Z][a-zA-Z]{2,15})(?:\\s+([A-Z][a-zA-Z]{1,15}))?\\b",
            RegexOption.IGNORE_CASE
        )
        financialPersonRegex.findAll(text).forEach { m ->
            val firstWord = m.groupValues[1].trim()
            val formattedFirst = firstWord.replaceFirstChar { it.uppercase() }
            val secondWord = m.groups[2]?.value?.trim()
            val candidate = if (!secondWord.isNullOrBlank() && isValidPersonName(secondWord, validKnownNames)) {
                "$formattedFirst ${secondWord.replaceFirstChar { it.uppercase() }}"
            } else {
                formattedFirst
            }
            if (isValidPersonName(candidate, validKnownNames)) {
                found.add(candidate)
            }
        }

        // 3. Explicit interpersonal phrases requiring capitalized Proper Names (e.g., "met with Sarah", "with Sarah", "talked to Marcus")
        val interpersonalCapitalizedRegex = Regex(
            "\\b(?:met\\s+with|talked\\s+(?:to|with)|spoke\\s+(?:to|with)|lunch\\s+with|dinner\\s+with|coffee\\s+with|meeting\\s+with|call\\s+with|called|with)\\s+([A-Z][a-z]{2,15})(?:\\s+([A-Z][a-z]{1,15}))?\\b"
        )
        interpersonalCapitalizedRegex.findAll(text).forEach { m ->
            val firstWord = m.groupValues[1].trim()
            val secondWord = m.groups[2]?.value?.trim()
            val candidate = if (!secondWord.isNullOrBlank() && isValidPersonName(secondWord, validKnownNames)) {
                "$firstWord $secondWord"
            } else {
                firstWord
            }
            if (isValidPersonName(candidate, validKnownNames)) {
                found.add(candidate)
            }
        }

        // 4. Subject patterns like "Jason owes me", "Jason paid me", "Jason lent me"
        val subjectRegex = Regex(
            "\\b([A-Za-z][a-zA-Z]{2,15})\\s+(?:owes\\s+me|owed\\s+me|paid\\s+me|repaid\\s+me|lent\\s+me|loaned\\s+me)\\b",
            RegexOption.IGNORE_CASE
        )
        subjectRegex.findAll(text).forEach { m ->
            val word = m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            if (isValidPersonName(word, validKnownNames)) {
                found.add(word)
            }
        }

        return found.filter { isValidPersonName(it, validKnownNames) }
    }

    /**
     * Checks if new input text reconciles or updates any existing notes in the user's database.
     * Returns a list of `DebriefNoteUpdate` items representing in-place modifications to existing notes.
     */
    fun findNoteUpdatesForInput(
        newInputText: String,
        existingNotes: List<NoteWithDetails>,
        knownPeopleNames: List<String> = emptyList()
    ): List<DebriefNoteUpdate> {
        val cleanInput = newInputText.replace(Regex("^\\[Debrief Mode:[^\\]]+\\]\\s*"), "").trim()
        if (cleanInput.isBlank() || existingNotes.isEmpty()) return emptyList()

        val allKnownNames = (knownPeopleNames + existingNotes.flatMap { it.people.map { p -> p.name } })
            .filter { isValidPersonName(it) }
            .distinct()
        val inputNames = extractCandidateNames(cleanInput, allKnownNames)
        val inputAmounts = extractMonetaryAmounts(cleanInput)
        val lowerInput = cleanInput.lowercase()

        val isPaymentOrDeduction = paymentDeltaKeywords.any { lowerInput.contains(it) }
        val isAddition = additionDeltaKeywords.any { lowerInput.contains(it) }
        val isDirectStateOverride = (lowerInput.contains("now owe") || lowerInput.contains("only owe") || lowerInput.contains("still owe") || lowerInput.contains("balance is now")) && !isPaymentOrDeduction
        val isSettledFull = lowerInput.contains("paid off") || lowerInput.contains("paid in full") || lowerInput.contains("settled up") || lowerInput.contains("nothing left")
        val hasStatusUpdateCue = statusUpdateKeywords.any { lowerInput.contains(it) }

        val updates = mutableListOf<DebriefNoteUpdate>()
        val dateStamp = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())

        // Split multi-sentence debriefs into clauses or inspect as a whole so each update finds its matching note
        for (existing in existingNotes) {
            // Never treat an identical note as an update to itself
            if (areNoteContentsEquivalent(existing.note.content, cleanInput)) continue

            val noteTextCombined = "${existing.note.title}\n${existing.note.content}"
            val lowerNote = noteTextCombined.lowercase()
            val noteNames = (existing.people.map { it.name }.filter { isValidPersonName(it) } +
                    extractCandidateNames(noteTextCombined, allKnownNames)).distinctBy { it.lowercase() }

            // Check if person matches
            val sharedPeople = inputNames.filter { inName ->
                noteNames.any { nName ->
                    nName.equals(inName, ignoreCase = true) ||
                            nName.lowercase().contains(inName.lowercase()) ||
                            inName.lowercase().contains(nName.lowercase())
                }
            }

            // Check topic token overlap
            val inputTokens = extractTopicTokens(cleanInput, inputNames)
            val noteTokens = extractTopicTokens(noteTextCombined, noteNames)
            val sharedTokens = inputTokens.intersect(noteTokens)

            val noteHasDebtContext = debtCreationKeywords.any { lowerNote.contains(it) } || paymentDeltaKeywords.any { lowerNote.contains(it) }
            val inputHasFinancialContext = isPaymentOrDeduction || isAddition || isDirectStateOverride || isSettledFull || debtCreationKeywords.any { lowerInput.contains(it) }

            // CASE 1: Financial / Balance Reconciliation (e.g. "I owe Jason 600 dollars" + "I paid Jason $200")
            if (noteHasDebtContext && inputHasFinancialContext && (sharedPeople.isNotEmpty() || sharedTokens.isNotEmpty())) {
                // Find the current balance in the existing note (prefer title first, then main content)
                val titleAmounts = extractMonetaryAmounts(existing.note.title)
                val bodyAmounts = extractMonetaryAmounts(existing.note.content)
                val currentBalanceExtraction = titleAmounts.firstOrNull() ?: bodyAmounts.firstOrNull()

                if (currentBalanceExtraction != null) {
                    val oldBalance = currentBalanceExtraction.amount
                    val deltaAmount = inputAmounts.firstOrNull()?.amount

                    if (deltaAmount != null || isSettledFull) {
                        val newBalance = when {
                            isSettledFull && deltaAmount == null -> 0.0
                            isDirectStateOverride && deltaAmount != null -> deltaAmount
                            isAddition && deltaAmount != null -> oldBalance + deltaAmount
                            isPaymentOrDeduction && deltaAmount != null -> (oldBalance - deltaAmount).coerceAtLeast(0.0)
                            deltaAmount != null && lowerInput.contains("paid") -> (oldBalance - deltaAmount).coerceAtLeast(0.0)
                            else -> null
                        }

                        if (newBalance != null && abs(newBalance - oldBalance) > 0.001) {
                            val personLabel = sharedPeople.firstOrNull() ?: noteNames.firstOrNull() ?: ""
                            val updatedTitle = rewriteTextWithNewAmount(
                                originalText = existing.note.title,
                                oldAmount = oldBalance,
                                newAmount = newBalance,
                                isTitle = true,
                                personName = personLabel
                            )
                            val primaryBodyWithoutHistory = existing.note.content
                                .lines()
                                .filterNot { it.trim().startsWith("🔄 Updated") || it.trim().startsWith("📝 Update History") }
                                .joinToString("\n")
                                .trim()
                            val existingHistoryLines = existing.note.content
                                .lines()
                                .filter { it.trim().startsWith("🔄 Updated") }

                            val rewrittenPrimaryBody = rewriteTextWithNewAmount(
                                originalText = primaryBodyWithoutHistory.ifBlank { existing.note.title },
                                oldAmount = oldBalance,
                                newAmount = newBalance,
                                isTitle = false,
                                personName = personLabel
                            )

                            val oldFormatted = formatCurrency(oldBalance)
                            val newFormatted = formatCurrency(newBalance)
                            val deltaFormatted = if (deltaAmount != null) formatCurrency(deltaAmount) else oldFormatted

                            val changeDescription = when {
                                newBalance == 0.0 -> "Settled in full (paid $deltaFormatted, was $oldFormatted → now $0)"
                                newBalance < oldBalance -> "Deducted $deltaFormatted payment (was $oldFormatted → now $newFormatted)"
                                else -> "Added $deltaFormatted (was $oldFormatted → now $newFormatted)"
                            }

                            val newHistoryEntry = "🔄 Updated ($dateStamp): $changeDescription — \"$cleanInput\""
                            val fullUpdatedHistory = (existingHistoryLines + newHistoryEntry).joinToString("\n")

                            val updatedContent = buildString {
                                append(rewrittenPrimaryBody)
                                append("\n\n")
                                append("📝 Update History:\n")
                                append(fullUpdatedHistory)
                            }

                            val mergedPeople = (existing.people.map { it.name } + sharedPeople + inputNames).distinctBy { it.lowercase() }
                            val existingTags = existing.note.getDisplayTagsList()
                            val mergedTags = (existingTags + listOf("updated", "finance")).distinct()

                            updates.add(
                                DebriefNoteUpdate(
                                    targetNoteId = existing.note.id,
                                    originalTitle = existing.note.title,
                                    updatedTitle = updatedTitle,
                                    originalContent = existing.note.content,
                                    updatedContent = updatedContent,
                                    category = if (existing.note.category == "General") "Finance" else existing.note.category,
                                    changeSummary = changeDescription,
                                    peopleInvolved = mergedPeople,
                                    tags = mergedTags
                                )
                            )
                            continue
                        }
                    }
                }
            }

            // CASE 2: General Factual / Status / Numeric Update on an Existing Note
            // Matches when the same person + status update cue is present,
            // OR high topic overlap with an explicit update/change statement, OR numeric replacement for same person+topic.
            val strongPersonAndTopicMatch = sharedPeople.isNotEmpty() && hasStatusUpdateCue && sharedTokens.isNotEmpty()
            val strongTopicUpdateMatch = sharedTokens.size >= 2 && hasStatusUpdateCue
            val numericReplacementMatch = sharedPeople.isNotEmpty() && sharedTokens.isNotEmpty() &&
                    inputAmounts.isNotEmpty() && extractMonetaryAmounts(noteTextCombined).isNotEmpty()

            if (strongPersonAndTopicMatch || strongTopicUpdateMatch || numericReplacementMatch) {
                // Avoid matching if the new input is identical to the existing note
                if (existing.note.content.contains(cleanInput, ignoreCase = true) ||
                    areNoteContentsEquivalent(existing.note.content, cleanInput)
                ) continue

                val oldAmounts = extractMonetaryAmounts(noteTextCombined)
                val newAmounts = inputAmounts

                val primaryBodyWithoutHistory = existing.note.content
                    .lines()
                    .filterNot { it.trim().startsWith("🔄 Updated") || it.trim().startsWith("📝 Update History") }
                    .joinToString("\n")
                    .trim()
                val existingHistoryLines = existing.note.content
                    .lines()
                    .filter { it.trim().startsWith("🔄 Updated") }

                val updatedTitle: String
                val updatedPrimaryBody: String
                val changeDesc: String

                if (oldAmounts.isNotEmpty() && newAmounts.isNotEmpty() && abs(oldAmounts.first().amount - newAmounts.first().amount) > 0.001) {
                    val oldVal = oldAmounts.first().amount
                    val newVal = newAmounts.first().amount
                    val personLabel = sharedPeople.firstOrNull() ?: ""
                    updatedTitle = rewriteTextWithNewAmount(existing.note.title, oldVal, newVal, isTitle = true, personName = personLabel)
                    updatedPrimaryBody = rewriteTextWithNewAmount(primaryBodyWithoutHistory, oldVal, newVal, isTitle = false, personName = personLabel) +
                            "\nLatest Note: $cleanInput"
                    changeDesc = "Updated amount from ${formatCurrency(oldVal)} to ${formatCurrency(newVal)}"
                } else {
                    updatedTitle = existing.note.title
                    updatedPrimaryBody = "$primaryBodyWithoutHistory\n\nLatest Update: $cleanInput"
                    changeDesc = "Updated note with latest information: \"${cleanInput.take(60)}\""
                }

                val newHistoryEntry = "🔄 Updated ($dateStamp): $changeDesc"
                val fullUpdatedHistory = (existingHistoryLines + newHistoryEntry).joinToString("\n")

                val updatedContent = buildString {
                    append(updatedPrimaryBody.trim())
                    append("\n\n")
                    append("📝 Update History:\n")
                    append(fullUpdatedHistory)
                }

                val mergedPeople = (existing.people.map { it.name } + sharedPeople + inputNames)
                    .filter { isValidPersonName(it, allKnownNames) }
                    .distinctBy { it.lowercase() }
                val mergedTags = (existing.note.getDisplayTagsList() + listOf("updated")).distinct()

                updates.add(
                    DebriefNoteUpdate(
                        targetNoteId = existing.note.id,
                        originalTitle = existing.note.title,
                        updatedTitle = updatedTitle,
                        originalContent = existing.note.content,
                        updatedContent = updatedContent,
                        category = existing.note.category,
                        changeSummary = changeDesc,
                        peopleInvolved = mergedPeople,
                        tags = mergedTags
                    )
                )
            }
        }

        return updates
    }

    /**
     * Enriches a `DailyDebriefResult` by reconciling it against `existingNotes`.
     * If `findNoteUpdatesForInput` detects that the debrief updates an existing note:
     * 1. Populates `noteUpdates` with the reconciled update(s).
     * 2. Updates `dailySummary` and `peopleUpdates` so the summary and person facts reflect the new reconciled amount (e.g. $400 instead of just repeating $200).
     * 3. Removes redundant new `distinctNotes` that merely duplicated the update or duplicate existing note content so the user never gets duplicate notes with different headings!
     */
    fun reconcileDebriefWithExistingNotes(
        rawTranscript: String,
        debriefResult: DailyDebriefResult,
        existingNotes: List<NoteWithDetails>,
        knownPeopleNames: List<String> = emptyList()
    ): DailyDebriefResult {
        val validKnownNames = knownPeopleNames.filter { isValidPersonName(it) }
        val textToAnalyze = rawTranscript.ifBlank { debriefResult.rawTranscription ?: debriefResult.dailySummary }
        val deterministicUpdates = findNoteUpdatesForInput(
            newInputText = textToAnalyze,
            existingNotes = existingNotes,
            knownPeopleNames = validKnownNames
        )

        // Merge any AI-detected noteUpdates with deterministicUpdates (preferring deterministic math accuracy when IDs match)
        val mergedNoteUpdates = mutableListOf<DebriefNoteUpdate>()
        mergedNoteUpdates.addAll(deterministicUpdates)
        for (aiUpdate in debriefResult.noteUpdates) {
            if (mergedNoteUpdates.none { it.targetNoteId == aiUpdate.targetNoteId }) {
                mergedNoteUpdates.add(
                    aiUpdate.copy(
                        peopleInvolved = aiUpdate.peopleInvolved.filter { isValidPersonName(it, validKnownNames) }
                    )
                )
            }
        }

        // Filter out any bogus people updates (verbs/nouns like "Have", "Saw", "Been")
        val updatedPeopleList = debriefResult.peopleUpdates
            .filter { isValidPersonName(it.personName, validKnownNames) }
            .toMutableList()

        // Deduplicate distinctNotes by normalized content so we never emit two notes with different headings and the same main content
        val uniqueDistinctNotes = debriefResult.distinctNotes
            .map { note ->
                note.copy(
                    peopleInvolved = note.peopleInvolved.filter { isValidPersonName(it, validKnownNames) }
                )
            }
            .distinctBy { normalizeNoteContentForDedup(it.content).ifBlank { it.title.lowercase().trim() } }

        if (mergedNoteUpdates.isEmpty()) {
            return debriefResult.copy(
                peopleUpdates = updatedPeopleList,
                distinctNotes = uniqueDistinctNotes
            )
        }

        // Update peopleUpdates so facts reflect the reconciled truth
        for (noteUpdate in mergedNoteUpdates) {
            for (personName in noteUpdate.peopleInvolved) {
                if (!isValidPersonName(personName, validKnownNames)) continue
                val existingPersonIdx = updatedPeopleList.indexOfFirst {
                    it.personName.equals(personName, ignoreCase = true)
                }
                val reconciledFact = "${noteUpdate.updatedTitle} (${noteUpdate.changeSummary})"
                if (existingPersonIdx != -1) {
                    val current = updatedPeopleList[existingPersonIdx]
                    val updatedFacts = (listOf(reconciledFact) + current.newFacts).distinct()
                    updatedPeopleList[existingPersonIdx] = current.copy(newFacts = updatedFacts)
                } else {
                    updatedPeopleList.add(
                        DebriefPersonUpdate(
                            personName = personName,
                            relationshipHint = "Contact",
                            newFacts = listOf(reconciledFact)
                        )
                    )
                }
            }
        }

        // Filter out redundant newly created notes when mergedNoteUpdates already updated the target note(s),
        // or when a distinctNote's content merely repeats the transcript/existing note.
        val filteredDistinctNotes = if (mergedNoteUpdates.isNotEmpty()) {
            uniqueDistinctNotes.filterNot { candidate ->
                val singleNoteWords = candidate.content.trim().split(Regex("\\s+")).size
                val matchesTranscript = areNoteContentsEquivalent(candidate.content, textToAnalyze)
                val matchesUpdatedNote = mergedNoteUpdates.any { upd ->
                    areNoteContentsEquivalent(candidate.content, upd.originalContent) ||
                        areNoteContentsEquivalent(candidate.content, upd.updatedContent)
                }
                singleNoteWords < 45 || matchesTranscript || matchesUpdatedNote
            }
        } else {
            uniqueDistinctNotes
        }

        val summarySuffix = mergedNoteUpdates.joinToString("; ") {
            "Updated '${it.originalTitle}' → '${it.updatedTitle}' (${it.changeSummary})"
        }
        val enhancedSummary = if (debriefResult.dailySummary.contains("Updated '", ignoreCase = true)) {
            debriefResult.dailySummary
        } else {
            "${debriefResult.dailySummary.trim()} [$summarySuffix]"
        }

        return debriefResult.copy(
            dailySummary = enhancedSummary,
            peopleUpdates = updatedPeopleList,
            distinctNotes = filteredDistinctNotes,
            noteUpdates = mergedNoteUpdates
        )
    }

    /**
     * Reconciles a person's existing facts list with new facts, replacing outdated numeric/balance facts
     * on the same subject (e.g., replacing "I owe Jason 600 dollars" with "I owe Jason $400 (Deducted $200 payment)").
     */
    fun reconcilePersonFacts(
        existingFacts: List<String>,
        incomingFacts: List<String>,
        personName: String
    ): List<String> {
        val mutableFacts = existingFacts.toMutableList()

        for (newFact in incomingFacts) {
            val lowerNew = newFact.lowercase()
            val newHasDebt = debtCreationKeywords.any { lowerNew.contains(it) } ||
                    paymentDeltaKeywords.any { lowerNew.contains(it) }
            val newAmounts = extractMonetaryAmounts(newFact)

            if (newHasDebt && newAmounts.isNotEmpty()) {
                // Remove older financial/debt facts for this person so the latest reconciled balance replaces them
                mutableFacts.removeAll { oldFact ->
                    val lowerOld = oldFact.lowercase()
                    val oldHasDebt = debtCreationKeywords.any { lowerOld.contains(it) } ||
                            paymentDeltaKeywords.any { lowerOld.contains(it) }
                    oldHasDebt && extractMonetaryAmounts(oldFact).isNotEmpty()
                }
            }
            if (mutableFacts.none { it.equals(newFact, ignoreCase = true) }) {
                mutableFacts.add(0, newFact)
            }
        }

        return mutableFacts
    }

    /**
     * Rewrites `originalText` by replacing the old numeric amount with `newAmount` in both "$600" and "600 dollars" formats.
     */
    fun rewriteTextWithNewAmount(
        originalText: String,
        oldAmount: Double,
        newAmount: Double,
        isTitle: Boolean,
        personName: String = ""
    ): String {
        val oldPlain = plainNumberFormatter.format(oldAmount)
        val oldComma = numberFormatter.format(oldAmount)
        val newFormattedDollar = formatCurrency(newAmount)
        val newPlain = plainNumberFormatter.format(newAmount)

        var updated = originalText

        // 1. Replace "$600" or "$600.00"
        val dollarPattern = Regex("\\$\\s*(?:${Regex.escape(oldComma)}|${Regex.escape(oldPlain)})(?:\\.00)?\\b")
        if (dollarPattern.containsMatchIn(updated)) {
            updated = dollarPattern.replaceFirst(updated, Regex.escapeReplacement(newFormattedDollar))
        } else {
            // 2. Replace "600 dollars" / "600 bucks"
            val wordPattern = Regex("\\b(?:${Regex.escape(oldComma)}|${Regex.escape(oldPlain)})(?:\\.00)?\\s*(dollars?|bucks?|usd)\\b", RegexOption.IGNORE_CASE)
            val wordMatch = wordPattern.find(updated)
            if (wordMatch != null) {
                val unit = wordMatch.groupValues[1]
                updated = wordPattern.replaceFirst(
                    updated,
                    Regex.escapeReplacement("$newPlain $unit ($newFormattedDollar)")
                )
            } else {
                // 3. Replace standalone old number
                val standalonePattern = Regex("\\b(?:${Regex.escape(oldComma)}|${Regex.escape(oldPlain)})(?:\\.00)?\\b")
                if (standalonePattern.containsMatchIn(updated)) {
                    updated = standalonePattern.replaceFirst(updated, Regex.escapeReplacement(newFormattedDollar))
                } else if (isTitle) {
                    updated = "$originalText ($newFormattedDollar)"
                }
            }
        }

        if (newAmount == 0.0 && isTitle && !updated.contains("Settled", ignoreCase = true)) {
            updated = "$updated (Settled)"
        }

        return updated
    }

    fun formatCurrency(amount: Double): String {
        return "$" + numberFormatter.format(amount)
    }

    private fun extractTopicTokens(text: String, personNames: List<String>): Set<String> {
        val nameParts = personNames.flatMap { it.lowercase().split(Regex("\\s+")) }.toSet()
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter {
                it.length >= 3 &&
                        it !in commonStopWords &&
                        it !in nameParts &&
                        it !in paymentDeltaKeywords &&
                        it !in statusUpdateKeywords &&
                        !it.all { ch -> ch.isDigit() }
            }
            .toSet()
    }
}
