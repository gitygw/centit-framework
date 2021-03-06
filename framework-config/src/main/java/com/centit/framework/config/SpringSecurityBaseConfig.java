package com.centit.framework.config;

import com.centit.framework.security.*;
import com.centit.framework.security.model.CentitSecurityMetadata;
import com.centit.framework.security.model.CentitSessionRegistry;
import com.centit.framework.security.model.CentitUserDetailsService;
import com.centit.support.algorithm.BooleanBaseOpt;
import com.centit.support.algorithm.StringBaseOpt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zou_wy on 2017/3/29.
 */
public abstract class SpringSecurityBaseConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    protected Environment env;

    @Autowired
    protected CsrfTokenRepository csrfTokenRepository;

    @Autowired
    protected CentitSessionRegistry centitSessionRegistry;

    @Autowired
    protected CentitUserDetailsService centitUserDetailsService;

    @Override
    public void configure(WebSecurity web) throws Exception {
        String ignoreUrl = StringUtils.deleteWhitespace(env.getProperty("security.ignore.url"));
        if(StringUtils.isNotBlank(ignoreUrl)){
            String[] ignoreUrls = ignoreUrl.split(",");
            for(int i = 0; i < ignoreUrls.length; i++){
                web.ignoring().antMatchers(ignoreUrls[i]);
            }
        }
        web.httpFirewall(httpFirewall());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        if(BooleanBaseOpt.castObjectToBoolean(env.getProperty("http.anonymous.disable"),false)) {
            http.anonymous().disable();
        }

        if(getPermitAllUrl() != null && getPermitAllUrl().length>0) {
            http.authorizeRequests().antMatchers(getPermitAllUrl()).permitAll();
        }
        if(getAuthenticatedUrl() != null && getAuthenticatedUrl().length>0) {
            http.authorizeRequests().antMatchers(getAuthenticatedUrl()).authenticated();
        }


        if(BooleanBaseOpt.castObjectToBoolean(env.getProperty("http.csrf.enable"),false)) {
            http.csrf().csrfTokenRepository(csrfTokenRepository);
        } else {
            http.csrf().disable();
        }

        http.exceptionHandling().accessDeniedPage("/system/exception/error/403");

        http.httpBasic().authenticationEntryPoint(getAuthenticationEntryPoint());

        switch (getFrameOptions()){
            case "DISABLE":
                http.headers().frameOptions().disable();
                break;
            case "SAMEORIGIN":
                http.headers().frameOptions().sameOrigin();
                break;
            default:
                http.headers().frameOptions().deny();
        }

        String defaultTargetUrl = env.getProperty("login.success.targetUrl");
        http.logout().logoutSuccessUrl(StringBaseOpt.emptyValue(defaultTargetUrl,"/"));


        http.addFilterAt(getAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(centitPowerFilter(), FilterSecurityInterceptor.class)
            .addFilterBefore(logoutFilter(), CasAuthenticationFilter.class);
    }

    protected abstract String[] getAuthenticatedUrl();

    protected abstract String[] getPermitAllUrl();

    protected abstract AuthenticationEntryPoint getAuthenticationEntryPoint();

    protected String getFrameOptions(){
        String frameOptions = env.getProperty("framework.x-frame-options.mode");
        frameOptions = StringBaseOpt.emptyValue(frameOptions,"deny").toUpperCase();
        return frameOptions;
    }

    protected abstract AbstractAuthenticationProcessingFilter getAuthenticationFilter();

    protected DaoFilterSecurityInterceptor centitPowerFilter(){
        AuthenticationManager authenticationManager = createAuthenticationManager();
        Assert.notNull(authenticationManager, "authenticationManager不能为空");
        AjaxAuthenticationSuccessHandler successHandler = createAjaxSuccessHandler();
        Assert.notNull(successHandler, "successHandler不能为空");
        AjaxAuthenticationFailureHandler failureHandler = createAjaxFailureHandler();
        Assert.notNull(failureHandler, "failureHandler不能为空");

        DaoFilterSecurityInterceptor securityInterceptor = new DaoFilterSecurityInterceptor();
        securityInterceptor.setAuthenticationManager(authenticationManager);
        securityInterceptor.setAccessDecisionManager(createCentitAccessDecisionManager());
        securityInterceptor.setSecurityMetadataSource(createCentitSecurityMetadataSource());
        securityInterceptor.setSessionRegistry(centitSessionRegistry);

        securityInterceptor.setAllResourceMustBeAudited(
            BooleanBaseOpt.castObjectToBoolean(
                env.getProperty("access.resource.notallowed.anonymous"),false));
        return securityInterceptor;
    }
    protected abstract Filter logoutFilter();


    protected AuthenticationManager createAuthenticationManager() {
        AuthenticationProvider authenticationProvider = getAuthenticationProvider();
        Assert.notNull(authenticationProvider, "authenticationProvider不能为空");
        List<AuthenticationProvider> providerList = new ArrayList<>();
        providerList.add(authenticationProvider);
        return new ProviderManager(providerList);
    }

    protected abstract AuthenticationProvider getAuthenticationProvider();

    protected AjaxAuthenticationFailureHandler createAjaxFailureHandler() {
        AjaxAuthenticationFailureHandler ajaxFailureHandler = new AjaxAuthenticationFailureHandler();
        String defaultTargetUrl = env.getProperty("login.failure.targetUrl");
        ajaxFailureHandler.setDefaultFailureUrl(
                StringBaseOpt.emptyValue(defaultTargetUrl,
                        "/system/mainframe/login/error"));
        ajaxFailureHandler.setWriteLog(
                BooleanBaseOpt.castObjectToBoolean(
                        env.getProperty("login.failure.writeLog"),false));
        return ajaxFailureHandler;
    }

    protected AjaxAuthenticationSuccessHandler createAjaxSuccessHandler() {
        AjaxAuthenticationSuccessHandler ajaxSuccessHandler = new AjaxAuthenticationSuccessHandler();
        String defaultTargetUrl = env.getProperty("login.success.targetUrl");
        ajaxSuccessHandler.setDefaultTargetUrl(StringBaseOpt.emptyValue(defaultTargetUrl,"/"));

        ajaxSuccessHandler.setWriteLog(BooleanBaseOpt.castObjectToBoolean(
                env.getProperty("login.success.writeLog"),true));
        ajaxSuccessHandler.setRegistToken(BooleanBaseOpt.castObjectToBoolean(
                env.getProperty("login.success.registToken"),false));
        ajaxSuccessHandler.setUserDetailsService(centitUserDetailsService);
        ajaxSuccessHandler.setSessionRegistry(centitSessionRegistry);
        return ajaxSuccessHandler;
    }

    protected DaoAccessDecisionManager createCentitAccessDecisionManager() {
        return new DaoAccessDecisionManager();
    }

    protected DaoInvocationSecurityMetadataSource createCentitSecurityMetadataSource() {
        CentitSecurityMetadata.setIsForbiddenWhenAssigned(
            BooleanBaseOpt.castObjectToBoolean(env.getProperty("access.resource.must.be.assigned"), false));
        return new DaoInvocationSecurityMetadataSource();
    }

    private StrictHttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(BooleanBaseOpt.castObjectToBoolean(env.getProperty("http.firewall.allowSemicolon"),true));
        return firewall;
    }

}
