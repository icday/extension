package com.daiyc.extension.util

import spock.lang.Specification;

/**
 * @author daiyc
 * @since 2024/8/12
 */
class NamingUtilsSpec extends Specification {
    def '测试统一化名称'(String src, String dst) {
        expect:
        ExtensionNamingUtils.unifyExtensionName(src) == dst

        where:
        src                 | dst
        "EMAIL"             | "email"
        "User_EMAIL"        | "user_email"
        "User_EMAIL"        | "user_email"
        "User_EMAIL2"       | "user_email2"
        "User_E_MAIL"       | "user_e_mail"
        "uSer_E_MAIL"       | "user_e_mail"
        "User-E_MAIL"       | "user_e_mail"
        "UserEMail"         | "user_e_mail"
        "UCBrowser"         | "uc_browser"
        "UcBrowser"         | "uc_browser"
    }
}
