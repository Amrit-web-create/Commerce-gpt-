package com.amrit.commercehub11

data class Chapter(val name: String, val url: String)
data class Book(val id: String, val title: String, val medium: String, val cover: String, val chapters: List<Chapter>)

val booksData: List<Book> = listOf(
    Book(
        id = "acc1_en",
        title = "Accountancy Part I (Class XI)",
        medium = "English Medium",
        cover = "acc1_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Introduction to Accounting", "https://drive.google.com/file/d/1SgxxxE6ckcNR8pm9Km77zGV2x68zSQTj/preview"),
            Chapter("Ch 2: Theory Base of Accounting", "https://drive.google.com/file/d/1YN2s4XSGQpKnpUmNNHdMhrZNlY9YeO0U/preview"),
            Chapter("Ch 3: Recording of Transactions - I", "https://drive.google.com/file/d/1ARX1zGG9jXSUC0F0-TqgE1ZHEXznZcS-/preview"),
            Chapter("Ch 4: Recording of Transactions - II", "https://drive.google.com/file/d/1PSCcnlejwTHsuOhdp3ryNJEgKcpJjRm5/preview"),
            Chapter("Ch 5: Bank Reconciliation Statement", "https://drive.google.com/file/d/1eYpMbXWyAS4CSyoWjaZdMcGaepfPwSf6/preview"),
            Chapter("Ch 6: Trial Balance and Rectification of Errors", "https://drive.google.com/file/d/1GadhbO6iPqKOo0j37ivRilNyCG9SZ4dn/preview"),
            Chapter("Ch 7: Depreciation, Provisions and Reserves", "https://drive.google.com/file/d/1Nt1cy3mTHXB7Y8eLbER-u6G9puj836bz/preview"),
        )
    ),
    Book(
        id = "acc1_hi",
        title = "लेखाशास्त्र भाग 1 (कक्षा XI)",
        medium = "Hindi Medium",
        cover = "acc1_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: लेखांकन परिचय", "https://drive.google.com/file/d/1OGG8G8pibJtBdmeDAQOLcwFeM0IsBm4k/preview"),
            Chapter("अध्याय 2: लेखांकन का सैद्धांतिक आधार", "https://drive.google.com/file/d/1zX8mauNEa82RcWRM-rAgxH0Pp0prnmjs/preview"),
            Chapter("अध्याय 3: लेन-देनों का अभिलेखन - I", "https://drive.google.com/file/d/1yGeUeI7XKIyIdjiIKnZ1bkh2FQuJXXCu/preview"),
            Chapter("अध्याय 4: लेन-देनों का अभिलेखन - II", "https://drive.google.com/file/d/15LoJi-yPcDukGwppokhhsv3YN32MgzG-/preview"),
            Chapter("अध्याय 5: बैंक समाधान विवरण", "https://drive.google.com/file/d/1LrqWTiSuEeYunWT0BrWSXbioWnb5xyLQ/preview"),
            Chapter("अध्याय 6: तलपट एवं अशुद्धियों का सुधार", "https://drive.google.com/file/d/13YS5xwf_hZkgTrdPFDD0RbPRydydPB7B/preview"),
            Chapter("अध्याय 7: ह्रास, प्रावधान तथा संचय", "https://drive.google.com/file/d/1gg3h4iq8OtNP-6bLUmxnnN0SDfhElQR6/preview"),
        )
    ),
    Book(
        id = "acc2_en",
        title = "Accountancy Part II (Class XI)",
        medium = "English Medium",
        cover = "acc2_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Financial Statements - I", "https://drive.google.com/file/d/15RCKACWESrxyeQ1TZdclPkOB34mcEGoR/preview"),
            Chapter("Ch 2: Financial Statements - II", "https://drive.google.com/file/d/1lP0eo1Js1W32qscrMJxMgi0udGnl2H7c/preview"),
        )
    ),
    Book(
        id = "acc2_hi",
        title = "लेखाशास्त्र भाग 2 (कक्षा XI)",
        medium = "Hindi Medium",
        cover = "acc2_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: वित्तीय विवरण - I", "https://drive.google.com/file/d/1EzIMEHNJkbeLcV4oCFMDW-CPTNu026jk/preview"),
            Chapter("अध्याय 2: वित्तीय विवरण - II", "https://drive.google.com/file/d/11zgIVL35RLtaa_q961GsU55p1kaQ_M3Z/preview"),
        )
    ),
    Book(
        id = "bst_en",
        title = "Business Studies (Class XI)",
        medium = "English Medium",
        cover = "bst_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Business, Trade and Commerce", "https://drive.google.com/file/d/1OaAt4nhtHXWpBKV-igRvMdzSMP9wIKiR/preview"),
            Chapter("Ch 2: Forms of Business Organisation", "https://drive.google.com/file/d/1b2g0by3PaPtCWZ6cqRAB0mVtUfrfU030/preview"),
            Chapter("Ch 3: Private, Public and Global Enterprises", "https://drive.google.com/file/d/1rqGr6HV_IQlsvRY7qP4k7BmYiSzJubZd/preview"),
            Chapter("Ch 4: Business Services", "https://drive.google.com/file/d/1Qu1kTWts7YpYGhMBgkVG9_F9o5FlVBg_/preview"),
            Chapter("Ch 5: Emerging Modes of Business", "https://drive.google.com/file/d/1UPNGuMi1V9jTqboJDX31U5wzDG4eMYZl/preview"),
            Chapter("Ch 6: Social Responsibilities of Business and Business Ethics", "https://drive.google.com/file/d/1VTToRV6SuxNAYBrTuvawFJF_Bbdqj4uE/preview"),
            Chapter("Ch 7: Formation of a Company", "https://drive.google.com/file/d/1jbvBDygQyta9gtf5c0jBKK5X_eL1vxnS/preview"),
            Chapter("Ch 8: Source of Business Finance", "https://drive.google.com/file/d/1MkZlxMneHlhegZzDb1W5HXC50Xpbkr7Q/preview"),
            Chapter("Ch 9: MSME and Business Entrepreneurship", "https://drive.google.com/file/d/1jssbcIwJ0Q1NVrJvAH9vDJi7xLriefXU/preview"),
            Chapter("Ch 10: Internal Trade", "https://drive.google.com/file/d/1BYRUW10PTwVo37O35igAZ6OFFRpEUBg7/preview"),
            Chapter("Ch 11: International Business", "https://drive.google.com/file/d/123rDH2rXFK5Uil7mRaMWv7882VBT9DI_/preview"),
        )
    ),
    Book(
        id = "bst_hi",
        title = "व्यवसाय अध्ययन (कक्षा XI)",
        medium = "Hindi Medium",
        cover = "bst_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: व्यवसाय, व्यापार और वाणिज्य", "https://drive.google.com/file/d/1fs-LELviF-0nH6XbCrMeMXPpcu4pOQzR/preview"),
            Chapter("अध्याय 2: व्यावसायिक संगठन के स्वरूप", "https://drive.google.com/file/d/1dkonpY5GR3tUJ1yZBZ1PPv3C0bHEOeSP/preview"),
            Chapter("अध्याय 3: निजी, सार्वजनिक एवं भूमंडलीय उपक्रम", "https://drive.google.com/file/d/1pTFxrgR9d3ecZFtz2eo8KHgQPMFxEQbY/preview"),
            Chapter("अध्याय 4: व्यावसायिक सेवाएँ", "https://drive.google.com/file/d/1zOPTvnmatI1xKhEmwNYKp0voZY-SMa-I/preview"),
            Chapter("अध्याय 5: व्यवसाय के उभरते हुए तरीके", "https://drive.google.com/file/d/1f6nRxL00ELpLOOHuaEGN1ztZl4Sh0Zy3/preview"),
            Chapter("अध्याय 6: व्यवसाय की सामाजिक उत्तरदायित्व तथा व्यावसायिक नैतिकता", "https://drive.google.com/file/d/1qpJYi1qjA4RLM1KywzobHIdQVPJsZgnD/preview"),
            Chapter("अध्याय 7: कंपनी की स्थापना", "https://drive.google.com/file/d/1xcxeEA6Eq1nxR39lXOj5Xynj8RC56Mj9/preview"),
            Chapter("अध्याय 8: व्यवसाय के वित्त के स्रोत", "https://drive.google.com/file/d/1anmPfR5YLJXM1Xx3X2gSo0bQUQvD-e6l/preview"),
            Chapter("अध्याय 9: सूक्ष्म, लघु एवं मध्यम उद्यम और व्यवसाय उद्यमिता", "https://drive.google.com/file/d/1njkMJ8MTt8m8zruvQSwQDd8z35VHzapG/preview"),
            Chapter("अध्याय 10: आंतरिक व्यापार", "https://drive.google.com/file/d/1qmDX_Zc_OiF6A6jKIc8kxUNvr-GLfsdL/preview"),
            Chapter("अध्याय 11: अंतर्राष्ट्रीय व्यापार", "https://drive.google.com/file/d/1jacVrI_BjMVE6ZgoEfyaKYHub451kCNL/preview"),
        )
    ),
    Book(
        id = "eco_ied_en",
        title = "Indian Economic Development",
        medium = "English Medium",
        cover = "eco_ied_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Indian Economy on the Eve of Independence", "https://drive.google.com/file/d/1B0v1ae_QAJ_HPhKSzV1nseFHQ_xNqDbv/preview"),
            Chapter("Ch 2: Indian Economy 1950-1990", "https://drive.google.com/file/d/1BaUS45Hz9Fs6RHzwnCfkBCcK9wc1ACKn/preview"),
            Chapter("Ch 3: Liberalisation, Privatisation and Globalisation", "https://drive.google.com/file/d/1WU1WIFFDjULW1nArHBxdVNOZdy-30o2q/preview"),
            Chapter("Ch 4: Human Capital Formation in India", "https://drive.google.com/file/d/1wRKXj_SXNp4rFTn2LSTHdFTOqy-AH-iT/preview"),
            Chapter("Ch 5: Rural Development", "https://drive.google.com/file/d/16AMSzc30of1apii6oqRy5euHKoJIOp9j/preview"),
            Chapter("Ch 6: Employment :Growth,Information and other Issues", "https://drive.google.com/file/d/1ZQH8njz6Q4VTJGLcKiBers3tAxYL88cc/preview"),
            Chapter("Ch 7: Environment and Sustainable Development", "https://drive.google.com/file/d/1W1o7J5EEV8szF9xEKX1VTRaIyPGiVmzH/preview"),
            Chapter("Ch 8: Comparative Development Experiences of India and its Neighbours", "https://drive.google.com/file/d/1D_mfDTCQPTGVadcMXNLOSPsmzMiR7vkm/preview"),
        )
    ),
    Book(
        id = "eco_ied_hi",
        title = "भारतीय आर्थिक विकास",
        medium = "Hindi Medium",
        cover = "eco_ied_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: स्वतंत्रता की पूर्व संध्या पर भारतीय अर्थव्यवस्था", "https://drive.google.com/file/d/1JU1fwZOxkTuiWgdANTFdbmHfVb_YImZX/preview"),
            Chapter("अध्याय 2: भारतीय अर्थव्यवस्था 1950-1990", "https://drive.google.com/file/d/1AW3ZXLpeRrfJfItdvpWwkV_ebmsGUDwI/preview"),
            Chapter("अध्याय 3: उदारीकरण, निजीकरण और वैश्वीकरण", "https://drive.google.com/file/d/1Fzzk5BUfnPaSGBcaTNYczIdhxpcEgmH2/preview"),
            Chapter("अध्याय 4: भारत में मानव पूंजी निर्माण", "https://drive.google.com/file/d/1Vd8UpCAGPtxW19FM4WlUl97smAOWzhj2/preview"),
            Chapter("अध्याय 5: ग्रामीण विकास", "https://drive.google.com/file/d/1JuQYVmD46JF8lQcp-f3-DW84pknOBmLH/preview"),
            Chapter("अध्याय 6: रोजगार - संवृद्धि, अनौपचारीकरण एवं अन्य मुद्दे", "https://drive.google.com/file/d/1hnto20CkN9wm3SyDcXvM5k4qmuYqEbfF/preview"),
            Chapter("अध्याय 7: पर्यावरण और धारणीय विकास", "https://drive.google.com/file/d/1rYHzvahCcqM5sUYd7m4EB8by6r0QhlEy/preview"),
            Chapter("अध्याय 8: भारत और पड़ोसी देशों के तुलनात्मक विकास अनुभव", "https://drive.google.com/file/d/1hb47m3sZ4akJoenbU5QtjjNWDIDxxbzQ/preview"),
        )
    ),
    Book(
        id = "eco_stats_en",
        title = "Statistics for Economics",
        medium = "English Medium",
        cover = "eco_stats_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Introduction to Statistics", "https://drive.google.com/file/d/1VLB3NMqkVitkIHfL879Cuq0cnVL2VgD8/preview"),
            Chapter("Ch 2: Collection of Data", "https://drive.google.com/file/d/1zS2tU1N4Dhr1ETNSWtYiP7zueWWwixmm/preview"),
            Chapter("Ch 3: Organisation of Data", "https://drive.google.com/file/d/1_I4PQhzyAdxrINvVFqBjcXR36ERNE_XS/preview"),
            Chapter("Ch 4: Presentation of Data", "https://drive.google.com/file/d/1HQlNmCxJR-pLOxoCaH5b3a30n6a1xVhF/preview"),
            Chapter("Ch 5: Measures of Central Tendency", "https://drive.google.com/file/d/1ddNJAaBp0UIZprK3yE_lrmDotX4xqKB1/preview"),
            Chapter("Ch 6: Correlation", "https://drive.google.com/file/d/1nwfHRtIHWu6Eg9pr_z41FqXN1R-fDY3O/preview"),
            Chapter("Ch 7: Index Numbers", "https://drive.google.com/file/d/1mjCLO7xDOTu-GVy-DWZidvKva4uZvDKS/preview"),
            Chapter("Ch 8: Use of Statistical Tools", "https://drive.google.com/file/d/11Qfrp2SEkHSWJNlPUlHSmrsjviYSKntQ/preview"),
        )
    ),
    Book(
        id = "eco_stats_hi",
        title = "अर्थशास्त्र के लिए सांख्यिकी",
        medium = "Hindi Medium",
        cover = "eco_stats_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: सांख्यिकी परिचय", "https://drive.google.com/file/d/1Oz7G37_6dwqNPYtku4BhFfujKwKRO2EL/preview"),
            Chapter("अध्याय 2: आंकड़ों का संकलन", "https://drive.google.com/file/d/1Mp193EHwT5W8gSbl1-kti5HaxEZSMmZ1/preview"),
            Chapter("अध्याय 3: आंकड़ों का संगठन", "https://drive.google.com/file/d/1sKVp-TuGU84dr_ZkV6RRjb1-x5yLq_DI/preview"),
            Chapter("अध्याय 4: आंकड़ों का प्रस्तुतिकरण", "https://drive.google.com/file/d/1PJiH-HE4LylT4YRNXGA5w_UMGTrTxBaN/preview"),
            Chapter("अध्याय 5: केंद्रीय प्रवृत्ति की माप", "https://drive.google.com/file/d/1RHKMfOmawRlGE-uU8BGTsInfpEs_vCBu/preview"),
            Chapter("अध्याय 6: सहसंबंध", "https://drive.google.com/file/d/1PsUCV9q3ExzWmlCKBzp-5XYzxeX_88ch/preview"),
            Chapter("अध्याय 7: सूचकांक", "https://drive.google.com/file/d/13SxfutPuEay7bgy5y7SLoQ562vJIvLR0/preview"),
            Chapter("अध्याय 8: सांख्यिकीय विधियों का प्रयोग", "https://drive.google.com/file/d/1Qki7a0pcy4WUVbF1XXU_XqUBrNv4eVI_/preview"),
        )
    ),
    Book(
        id = "maths_en",
        title = "Mathematics (Class XI)",
        medium = "English Medium",
        cover = "maths_en_cover.jpg",
        chapters = listOf(
            Chapter("Ch 1: Sets", "https://drive.google.com/file/d/1zuIFGb7v4ADi49zHfdxyavB6m5-PCEYz/preview"),
            Chapter("Ch 2: Relations and Functions", "https://drive.google.com/file/d/1ED1-hPiunJ8qWzaSa3HQLW94J3lcoro5/preview"),
            Chapter("Ch 3: Trigonometric Functions", "https://drive.google.com/file/d/1H76qsVEG089EoGNQpkGI_d3dG_ZM7kj8/preview"),
            Chapter("Ch 4: Complex Numbers & Quadratic Equations", "https://drive.google.com/file/d/1CtmR3V8syjS3PQHYIILaGBM5GwvmVyXx/preview"),
            Chapter("Ch 5: Linear Inequalities", "https://drive.google.com/file/d/1sKF0pN4sjkJJVLuauKlksJodKLo-GSOq/preview"),
            Chapter("Ch 6: Permutations and Combinations", "https://drive.google.com/file/d/1zkVQ_obDJEHGLM2-v1jEVS5H9rTh4Tqj/preview"),
            Chapter("Ch 7: Binomial Theorem", "https://drive.google.com/file/d/15n9Xs7YksjAxVFRhfcyU58knoG8_JGtX/preview"),
            Chapter("Ch 8: Sequence and Series", "https://drive.google.com/file/d/1OpSceO_U0ZP12xXbsFs3FRpGrhgjxg5X/preview"),
            Chapter("Ch 9: Straight Lines", "https://drive.google.com/file/d/1t_Cao_VFc1XQSo3GvFA2nAXKnqm8yL5l/preview"),
            Chapter("Ch 10: Conic Sections", "https://drive.google.com/file/d/1NB9dRc2v25fVqCHn1H42m2eOQHtX0LuK/preview"),
            Chapter("Ch 11: Three Dimensional Geometry", "https://drive.google.com/file/d/1zArIxd1qspCww9qRHYxNt98X7oBhd8ER/preview"),
            Chapter("Ch 12: Limits and Derivatives", "https://drive.google.com/file/d/1Q3tO1fo9Gc2YtEdj2ZtloL1vPnRkjXRF/preview"),
            Chapter("Ch 13: Statistics", "https://drive.google.com/file/d/185yoLkgWChOicEt3H91-IxpEvxfOloxz/preview"),
            Chapter("Ch 14: Probability", "https://drive.google.com/file/d/1PlZAnfqvHUhW_obcYbsHedC6e8GoD9mr/preview"),
            Chapter("Answer Key (Solutions)", "https://drive.google.com/file/d/1uBDPAb6D-6rj0cKb1Zzq-20gixQGXdJS/preview"),
        )
    ),
    Book(
        id = "maths_hi",
        title = "गणित (कक्षा XI)",
        medium = "Hindi Medium",
        cover = "maths_hi_cover.jpg",
        chapters = listOf(
            Chapter("अध्याय 1: समुच्चय", "https://drive.google.com/file/d/1fIAtWh4Oyw9iT4mTCIZbdvNYDFOOsTaf/preview"),
            Chapter("अध्याय 2: संबंध एवं फलन", "https://drive.google.com/file/d/1iEhlZuYRoZCevX3xc_nnIDOpXCVqNh7o/preview"),
            Chapter("अध्याय 3: त्रिकोणमिति फलन", "https://drive.google.com/file/d/13diGW-V0t5gXiyxvdpQUC2tVCi30Ecbz/preview"),
            Chapter("अध्याय 4: सम्मिश्र संख्याएँ और द्विघात समीकरण", "https://drive.google.com/file/d/1LY4TBfsAfYMGWXaXkVhsxwlzVv7Prn41/preview"),
            Chapter("अध्याय 5: रैखिक असमिकाएँ", "https://drive.google.com/file/d/1COE9t7b5eTk_WdJn_aN61N9ZoofkGVPG/preview"),
            Chapter("अध्याय 6: क्रमचय और संचय", "https://drive.google.com/file/d/1Z-Wc4L1Nsdgb8V0uwkNsR-PnCr0sUhox/preview"),
            Chapter("अध्याय 7: द्विपद प्रमेय", "https://drive.google.com/file/d/1VTz_WYWoSU5KZwmuDjPmMaWgTT-Vy-MO/preview"),
            Chapter("अध्याय 8: अनुक्रम तथा श्रेणी", "https://drive.google.com/file/d/1OtxtdI0nlFNZdJ84PLVhfepKUrq6hxpS/preview"),
            Chapter("अध्याय 9: सरल रेखाएँ", "https://drive.google.com/file/d/1xW_jRBn6l2LwueQ9nZLQhDZ_SxhFzePb/preview"),
            Chapter("अध्याय 10: शंखु परिच्छेद", "https://drive.google.com/file/d/1whvL_1AK2TqS3hqTnFNYOE393qo5nHEW/preview"),
            Chapter("अध्याय 11: त्रिविमीय ज्यामिति", "https://drive.google.com/file/d/1cuAMO0ae4CI2nF7q4LhG2Bdc514dHmgv/preview"),
            Chapter("अध्याय 12: सीमा और अवकलज", "https://drive.google.com/file/d/1HsL8LiC7skKa-fahZlcYFsfgNXsq95hj/preview"),
            Chapter("अध्याय 13: सांख्यिकी एवं प्रायिकता", "https://drive.google.com/file/d/1NDEEGUNBA0xq-ikhIWFZ0mfI4WoA1heI/preview"),
            Chapter("अध्याय 14: गणितीय विवेचन", "https://drive.google.com/file/d/1HiJ8i74wKBKqS5eiwgECQAnmOVYSbWD0/preview"),
            Chapter("उत्तरमाला (Answer Key)", "https://drive.google.com/file/d/1Nh2f80twN0bhb23K2bjENgi1xh3FsCie/preview"),
        )
    ),
    Book(
        id = "lang_eng",
        title = "English Core (Hornbill & Snapshots)",
        medium = "English",
        cover = "lang_eng_cover.jpg",
        chapters = listOf(
            Chapter("Hornbill Ch 1: The Portrait of a Lady", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("Hornbill Ch 2: We’re Not Afraid to Die...", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("Hornbill Ch 3: Discovering Tut", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("Snapshots Ch 1: The Summer of the Beautiful White Horse", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("Snapshots Ch 2: The Address", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
        )
    ),
    Book(
        id = "lang_hindi",
        title = "हिन्दी कोर (आरोह और वितान)",
        medium = "Hindi",
        cover = "lang_hindi_cover.jpg",
        chapters = listOf(
            Chapter("आरोह पाठ 1: नमक का दरोगा", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("आरोह पाठ 2: मियाँ नसीरुद्दीन", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("आरोह पाठ 3: अप्पू के साथ ढाई साल", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("वितान पाठ 1: भारतीय गायिकाओं में बेजोड़ - लता मंगेशकर", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
            Chapter("वितान पाठ 2: राजस्थान की रजत बूंदें", "YOUR_DRIVE_DIRECT_LINK_HERE/preview"),
        )
    ),
)
