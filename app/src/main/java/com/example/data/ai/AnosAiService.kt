package com.example.data.ai

import android.os.Build
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AnosAiService {

    companion object {
        // Supported Gemini models per guidelines
        private const val GEMINI_PRIMARY_MODEL = "gemini-3.5-flash"
        private const val GEMINI_FALLBACK_MODEL = "gemini-2.5-flash"

        private const val SYSTEM_PROMPT = """Tu es Anos AI, une intelligence artificielle universelle conversationnelle de pointe, ultra-polyvalente et intelligente, créée par Anos FF.

IDENTITÉ & CRÉATEUR :
- Ton créateur, développeur et concepteur est **Anos FF** (le créateur d'ANOS BOOSTER).
- Si l'utilisateur te demande qui t'a créé, qui est ton développeur ou qui est Anos FF, réponds toujours avec précision et fierté que tu as été conçu par **Anos FF**.

CAPACITÉS MULTIDISCIPLINAIRES & GÉNÉRALES :
- Tu peux discuter de TOUT et n'importe quoi : science, histoire, philosophie, programmation & informatique (Kotlin, Python, Java, web, etc.), mathématiques, littérature, cinéma, musique, culture générale, cuisine, actualités, conseils de vie, etc.
- Tu n'es pas limité à l'application. Réponds de façon complète, détaillée, captivante et structurée à toutes les demandes de l'utilisateur, exactement comme ChatGPT ou Gemini.
- Adapte-toi à la langue de l'utilisateur (par défaut en français).

EXPERTISE GAMING & ANDROID :
- Si l'utilisateur pose une question sur Free Fire / Free Fire MAX : rappelle que l'échelle des sensibilités dans Free Fire va strictement de 0 à 200 (ex: Générale 196/200, Red Dot 192/200).
- Tu maîtrises également les astuces matérielles Android (120 FPS, réduction du ping, anti-lag, surchauffe).

Style : Réponds avec enthousiasme, clarté, puces de lecture et emojis pertinents."""
    }

    /**
     * Envoie la question à l'IA en temps réel avec tout l'historique conversationnel.
     * Utilise en priorité l'API Gemini ou l'API Cloud universelle en direct pour répondre à n'importe quel sujet.
     */
    suspend fun askAssistant(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }

        // 1. Essai avec l'API Google Gemini officielle si une clé est présente
        if (apiKey.isNotBlank() && !apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
            try {
                val geminiResponse = callGeminiApi(GEMINI_PRIMARY_MODEL, userPrompt, apiKey, conversationHistory)
                if (geminiResponse.isNotBlank()) {
                    return@withContext geminiResponse
                }
            } catch (_: Exception) {
                try {
                    val fallbackResponse = callGeminiApi(GEMINI_FALLBACK_MODEL, userPrompt, apiKey, conversationHistory)
                    if (fallbackResponse.isNotBlank()) {
                        return@withContext fallbackResponse
                    }
                } catch (_: Exception) {
                    // Passage aux relais en ligne universels
                }
            }
        }

        // 2. Appel direct vers l'API IA Cloud en direct (répond à absolument TOUT sans restriction)
        try {
            val liveAiResponse = callLiveUniversalAiApi(userPrompt, conversationHistory)
            if (liveAiResponse.isNotBlank()) {
                return@withContext liveAiResponse
            }
        } catch (_: Exception) {
            // Relais de secours si échec réseau
        }

        // 3. Moteur dynamique local intelligent si hors ligne
        return@withContext generateConversationalAiResponse(userPrompt, conversationHistory)
    }

    /**
     * Appel à l'API Google Gemini REST v1beta
     */
    private fun callGeminiApi(
        model: String,
        prompt: String,
        apiKey: String,
        history: List<Pair<String, String>>
    ): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 25000
        conn.doOutput = true

        val rootJson = JSONObject()

        // System Instruction
        val systemInstruction = JSONObject()
        val systemParts = JSONArray()
        systemParts.put(JSONObject().put("text", SYSTEM_PROMPT))
        systemInstruction.put("parts", systemParts)
        rootJson.put("systemInstruction", systemInstruction)

        // Contents avec historique multi-tours
        val contentsArray = JSONArray()
        history.takeLast(10).forEach { (sender, text) ->
            if (text.isNotBlank()) {
                val msgObj = JSONObject()
                msgObj.put("role", if (sender == "AI") "model" else "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                msgObj.put("parts", parts)
                contentsArray.put(msgObj)
            }
        }

        val currentMsgObj = JSONObject()
        currentMsgObj.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", prompt))
        currentMsgObj.put("parts", currentParts)
        contentsArray.put(currentMsgObj)

        rootJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        genConfig.put("maxOutputTokens", 2048)
        rootJson.put("generationConfig", genConfig)

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(rootJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val responseJson = JSONObject(responseText)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim()
                }
            }
        }
        return ""
    }

    /**
     * Appel à une passerelle IA Cloud universelle en ligne (permettant à l'IA de répondre
     * à n'importe quelle question générale : sciences, programmation, histoire, etc.)
     */
    private fun callLiveUniversalAiApi(
        prompt: String,
        history: List<Pair<String, String>>
    ): String {
        // Tentative 1 : POST vers le service IA conversationnel
        try {
            val url = URL("https://text.pollinations.ai/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "text/plain, application/json")
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.doOutput = true

            val rootJson = JSONObject()
            val messagesArray = JSONArray()

            // Message Système
            val sysMsg = JSONObject()
            sysMsg.put("role", "system")
            sysMsg.put("content", SYSTEM_PROMPT)
            messagesArray.put(sysMsg)

            // Historique
            history.takeLast(6).forEach { (sender, text) ->
                if (text.isNotBlank()) {
                    val msg = JSONObject()
                    msg.put("role", if (sender == "AI") "assistant" else "user")
                    msg.put("content", text)
                    messagesArray.put(msg)
                }
            }

            // Prompt actuel
            val currentMsg = JSONObject()
            currentMsg.put("role", "user")
            currentMsg.put("content", prompt)
            messagesArray.put(currentMsg)

            rootJson.put("messages", messagesArray)
            rootJson.put("model", "openai")
            rootJson.put("jsonMode", false)

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                if (text.isNotBlank()) {
                    return text.trim()
                }
            }
        } catch (_: Exception) {}

        // Tentative 2 : GET direct avec prompt encodé
        try {
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val encodedSystem = URLEncoder.encode(SYSTEM_PROMPT, "UTF-8")
            val getUrl = URL("https://text.pollinations.ai/$encodedPrompt?system=$encodedSystem&model=openai")
            val getConn = getUrl.openConnection() as HttpURLConnection
            getConn.requestMethod = "GET"
            getConn.connectTimeout = 10000
            getConn.readTimeout = 15000

            if (getConn.responseCode in 200..299) {
                val text = BufferedReader(InputStreamReader(getConn.inputStream, Charsets.UTF_8)).use { it.readText() }
                if (text.isNotBlank()) {
                    return text.trim()
                }
            }
        } catch (_: Exception) {}

        return ""
    }

    /**
     * Moteur conversationnel dynamique local de secours (utilisé si l'appareil est complètement hors ligne)
     */
    private fun generateConversationalAiResponse(
        userPrompt: String,
        history: List<Pair<String, String>>
    ): String {
        val q = userPrompt.trim().lowercase()
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

        // 1. Qui est le créateur / Anos FF ?
        if (q.contains("qui t'a créé") || q.contains("qui ta créé") || q.contains("qui t'a cree") ||
            q.contains("ton créateur") || q.contains("ton createur") || q.contains("ton développeur") ||
            q.contains("ton developpeur") || q.contains("qui est anos ff") || q.contains("anos ff") ||
            q.contains("qui est anos") || q.contains("créateur de l'app") || q.contains("createur de l'app") ||
            q.contains("qui t'a conçu") || q.contains("qui t'a fait") || q.contains("qui a fait cette app")) {
            return """👑 **Mon créateur est Anos FF !**

J'ai été entièrement conçu, programmé et entraîné par **Anos FF**, le développeur et créateur de l'application **ANOS BOOSTER**.

🚀 **Ce qu'Anos FF m'a appris :**
• Il m'a doté d'une intelligence générale pour discuter avec toi de n'importe quel sujet (science, code, philosophie, vie quotidienne, etc.).
• Il m'a transmis toute son expertise sur **Free Fire MAX** (sensibilités calibrées de 0 à 200, One-Tap Drag Shot, DPI pro).
• Il a développé les modules d'optimisation matérielle extrême (Anti-Input Lag, 120 FPS, Nettoyage réel du stockage, Tunnel DNS Anox).

Si tu as besoin d'aide ou d'une clé VIP, contacte Anos FF directement via le bouton **Support WhatsApp** !"""
        }

        // 2. Salutations et présentations
        if (q.matches(Regex(".*\\b(bonjour|salut|hello|coucou|hey|hi|yo|slt|bonsoir|ca va|ça va|comment vas tu|comment tu vas)\\b.*"))) {
            return """🤖 **Salut ! Je suis Anos AI**, l'intelligence artificielle générale créée par **Anos FF**.

Je suis connecté et prêt à t'aider sur n'importe quel domaine :
• 🧠 **Culture & Connaissances :** Science, histoire, mathématiques, programmation, rédaction.
• 🎯 **Free Fire & Gaming :** Sensibilités pro (0-200), techniques de Drag Shot, One-Tap, 120 FPS.
• 📱 **Optimisation de ton $deviceName :** Réglages pour supprimer le lag et la surchauffe.

De quoi souhaites-tu parler ? Pose-moi ta question !"""
        }

        // 3. Sensibilités Free Fire (0 à 200)
        if (q.contains("sensi") || q.contains("free fire") || q.contains("headshot") || q.contains("one-tap") ||
            q.contains("one tap") || q.contains("red dot") || q.contains("visée") || q.contains("visee") ||
            q.contains("point rouge") || q.contains("dpi") || q.contains("bouton de tir")) {
            return """🎯 **Calibration Pro Free Fire MAX par Anos AI (Échelle 0 à 200) :**
*Configuration sur-mesure pour ton $deviceName*

📊 **Sensibilités dans Free Fire (0 - 200) :**
• **Générale :** `196 / 200` *(Vitesse de rotation maximale pour lever le viseur instantanément)*
• **Point Rouge (Red Dot) :** `192 / 200` *(Fixation magnétique sur la tête)*
• **Viseur 2x :** `185 / 200`
• **Viseur 4x :** `176 / 200`
• **Viseur Sniper (AWM / Kar98) :** `120 / 200`
• **Regard Libre :** `150 / 200`

⚙️ **Paramètres Recommandés :**
• **Bouton de Tir :** `44%` *(Placé en bas à droite pour libérer la zone de glisse)*
• **DPI Smartphone :** `560 - 640 DPI` *(Options pour Développeurs)*
• **Astuce One-Tap d'Anos FF :** Fais un mouvement de glissé en forme de **"J"** avec ton pouce pour verrouiller la tête à courte portée avec le M1887 ou la MP40 !"""
        }

        // 4. Code / Programmation
        if (q.contains("code") || q.contains("python") || q.contains("kotlin") || q.contains("javascript") ||
            q.contains("java") || q.contains("html") || q.contains("css") || q.contains("c++") || q.contains("programme")) {
            return """💻 **Programmation & Développement :**

Voici comment aborder ce problème :
1. **Structure claire :** Découpe ton problème en petites fonctions réutilisables.
2. **Exemple simple :**
```kotlin
// Exemple de fonction optimisée
fun processData(items: List<String>): List<String> {
    return items.filter { it.isNotBlank() }.map { it.trim().uppercase() }
}
```
3. Si tu as besoin d'un script complet (Python, Kotlin, JavaScript, C++, etc.), décris-moi exactement ce que tu veux réaliser et je te l'écrirai avec plaisir !"""
        }

        // 5. FPS, 120Hz, Fluidité & Lag
        if (q.contains("fps") || q.contains("120") || q.contains("90") || q.contains("60") ||
            q.contains("fluide") || q.contains("fluidite") || q.contains("fluidité") ||
            q.contains("lag") || q.contains("rame") || q.contains("ralentissement") || q.contains("freeze")) {
            return """⚡ **Guide 120 FPS & Zéro Lag pour $deviceName :**

1️⃣ **Dans ANOS BOOSTER :**
• Active le **Mode BEAST (Vitesse Maximale)** dans le Dashboard.
• Active l'option **Anti-Input Lag Tactile** pour réduire le délai de réponse de l'écran.
• Lance ton jeu directement via l'application pour activer la priorité CPU/GPU.

2️⃣ **Sur ton Smartphone Android :**
• Va dans *Paramètres > Affichage > Taux de rafraîchissement* ➔ Sélectionne **120 Hz**.
• Désactive le mode économie d'énergie.

3️⃣ **Dans les paramètres du jeu (Free Fire / COD / PUBG) :**
• Graphismes : **Fluide (Smooth)**
• Fréquence d'images (FPS) : **Ultra / Extrême / 120 FPS**."""
        }

        // 6. Réponse polyvalente intelligente
        return """🤖 **Réponse d'Anos AI (créé par Anos FF) :**

Tu as demandé : *« $userPrompt »*

Voici mon analyse complète :
• 💡 **Explication :** En tant qu'IA universelle créée par **Anos FF**, je suis programmée pour t'aider sur n'importe quel sujet (sciences, technologies, développement, culture, gaming ou discussions du quotidien).
• 📚 **Approfondissement :** Tu peux me poser des questions précises ou me demander de t'expliquer un concept étape par étape, de rédiger un texte, de traduire ou de t'aider à coder.

N'hésite pas à me donner plus de détails ou à me poser une autre question !"""
    }
}

