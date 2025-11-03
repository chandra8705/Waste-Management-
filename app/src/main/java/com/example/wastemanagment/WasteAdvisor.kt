package com.example.wastemanagment

object WasteAdvisor {

    data class DisposalAdvice(
        val category: String,
        val advice: String,
        val icon: String = "♻️"
    )

    fun getDisposalAdvice(category: String): DisposalAdvice {
        return when (category.lowercase()) {
            "glass" -> DisposalAdvice(
                category = "Glass",
                advice = "Recycle at glass recycling unit. Clean the glass and remove any metal lids before recycling.",
                icon = "🔗"
            )

            "biodegradable", "organic" -> DisposalAdvice(
                category = "Biodegradable/Organic",
                advice = "Can be composted into manure. Create a compost pile or use a compost bin for natural decomposition.",
                icon = "🌱"
            )

            "non-biodegradable", "inorganic" -> DisposalAdvice(
                category = "Non-Biodegradable/Inorganic",
                advice = "Dispose in dry waste bin, consider recycling. Check if materials can be processed at recycling centers.",
                icon = "🗑️"
            )

            "plastic" -> DisposalAdvice(
                category = "Plastic",
                advice = "Sort by plastic type and recycle appropriately. Clean containers before recycling.",
                icon = "♻️"
            )

            "metal" -> DisposalAdvice(
                category = "Metal",
                advice = "Clean and recycle at metal recycling facility. Separate different metal types if possible.",
                icon = "🔧"
            )

            "paper", "cardboard" -> DisposalAdvice(
                category = "Paper/Cardboard",
                advice = "Recycle in paper recycling bin. Remove any plastic tape or staples before recycling.",
                icon = "📄"
            )

            "trash", "general waste" -> DisposalAdvice(
                category = "General Waste",
                advice = "Dispose in general waste bin. Consider if any parts can be separated for recycling.",
                icon = "🗑️"
            )

            else -> DisposalAdvice(
                category = "Unknown",
                advice = "Unable to classify waste type. Please dispose responsibly and consider consulting local waste management guidelines.",
                icon = "❓"
            )
        }
    }

    fun getAllCategories(): List<String> {
        return listOf(
            "Glass",
            "Biodegradable",
            "Non-Biodegradable",
            "Organic",
            "Inorganic",
            "Plastic",
            "Metal",
            "Paper",
            "Cardboard"
        )
    }
}