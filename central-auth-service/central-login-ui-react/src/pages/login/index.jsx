import React, { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import apiProvider from '../../services/API/IndividualApis';
import { useForm, FormProvider } from "react-hook-form";
import { Text } from "../../components/Text";
import { Button } from "../../components/Button";
import { Img } from "../../components/Img";
import { FloatingInput } from "../../components/FloatingInput";
import '../../App.css';
import SuiteLogos from '../../components/SuiteLogos';
import PSLogo from '../../components/PSLogo';



const LoginPage = ({search}) => {

    const [error, setError] = useState('');
    const [showLoader, setShowLoader] = useState(false);
    const [showSAMLLoader, setShowSAMLLoader] = useState(false);
    const methods = useForm({ mode: 'all' });

    const PerformSAMLLogin = () => {
        setShowSAMLLoader(true);
        window.location.href = apiProvider.handleSamlLogin;
    }

    const PerformCredentialLogin = (data) => {
        setShowLoader(true);
        apiProvider.handleUserStandardLogin({
            username: data.userName,
            password: data.password
        })
        .then((response) => {
            if(response){
                apiProvider.getStandardLoginStatus()
                .then((res) => {
                    if(res && res.data['success']){
                        const authToken = res.data.data.authToken;
                        const redirectUri = JSON.parse(localStorage.getItem('redirect_uri'));
                        localStorage.setItem('user_details', JSON.stringify({ email: res.data.data.email, isAuthenticated: true }));
                        setShowLoader(false);
                        let defaultAppUrl = process.env.NODE_ENV === 'production' ? window.env.REACT_APP_PSKnowHOW : process.env.REACT_APP_PSKnowHOW;
                        if(!redirectUri){
                            window.location.href = (defaultAppUrl + '?authToken=' + authToken);
                        }else{
                            if(redirectUri.indexOf('?') === -1){
                                window.location.href = (`${redirectUri}?authToken=${authToken}`);
                            }else{
                                window.location.href = (`${redirectUri}&authToken=${authToken}`);
                            }
                        }
                    } else {
                     setShowLoader(false);
                     setError(res.data.message);
                     }
                }).catch((err) => {
                    console.log(err);
                    setShowLoader(false);
                    let errMessage = err?.response?.data?.message ?  err?.response?.data?.message : 'Please try again after sometime'
                    setError(errMessage);
                });
            }
        })
        .catch((err) => {
            console.log(err);
            setShowLoader(false);
            let errMessage = err?.response?.data?.message ?  err?.response?.data?.message : 'Please try again after sometime'
            setError(errMessage);
        });
    }

    useEffect(() => {
        const redirectUri = search?.split("redirect_uri=")?.[1];
        if (redirectUri) {
            localStorage.setItem('redirect_uri', JSON.stringify(redirectUri));
        }
        return () => { };
    }, [search]);

    return (
        <div className="componentContainer flex h-screen max-w-screen">
            <SuiteLogos/>
            <div className="w-2/5 p-12 h-screen bg-white-A700">
                
                <PSLogo/>
                <div className='w-full mt-4 mb-2'>
                    <Text
                        className="text-left text-lg"
                        size="txtPoppinsBold44"
                    >
                        Welcome back!
                    </Text>
                </div>
                <Button
                    className="cursor-pointer flex min-h-[36px] items-center justify-center ml-0.5 md:ml-[0] mt-[18px] w-full"
                    rightIcon={
                        <>
                            <Img
                                className="h-5 mb-px ml-2"
                                src="images/img_arrowright.svg"
                                alt="arrow_right"
                            />
                            {showSAMLLoader && <Img
                                src={`${process.env.PUBLIC_URL}/images/spinner.png`} height='20'
                                className="spinner mb-px ml-2"
                                alt={`Spinner`}
                            />}
                        </>
                    } clickFn={PerformSAMLLogin}
                >
                    <Text className="text-white text-left">
                        Login with SSO
                    </Text>
                </Button>
                <Text className='text-left mt-4' size='txtPoppinsRegular16'>Or login with credentials</Text>
                <FormProvider {...methods}>
                    <form noValidate autoComplete='off'>
                        <FloatingInput type="text" placeHolder="User Name" id="userName" className={`mt-4 ${(methods.formState.errors['userName']) ? 'Invalid' : ''}`}
                            validationRules={{
                                "required": "Field is required",
                                "minLength": {
                                    "value": 6,
                                    "message": "Field should contain at least 6 characters"
                                }
                            }}>
                        </FloatingInput>
                        {(methods.formState.errors['userName']) && <p className='errMsg'>{methods.formState.errors['userName'].message}</p>}
                        <FloatingInput type="password" placeHolder="Password" id="password" className={`mt-4 ${(methods.formState.errors['password']) ? 'Invalid' : ''}`}
                            validationRules={{
                                "required": "Field is required",
                                "minLength": {
                                    "value": 6,
                                    "message": "Field should contain at least 6 characters"
                                }
                            }}>
                        </FloatingInput>
                        {(methods.formState.errors['password']) && <p className='errMsg'>{methods.formState.errors['password'].message}</p>}
                        
                        <Button
                            color='blue_80'
                            variant='fill'
                            className="cursor-pointer flex min-h-[36px] items-center justify-center ml-0.5 md:ml-[0] mt-[18px] w-full"
                            clickFn={methods.handleSubmit(PerformCredentialLogin)}
                            rightIcon={
                                <>
                                    <Img
                                        className="h-5 mb-px ml-2"
                                        src="images/img_arrowright.svg"
                                        alt="arrow_right"
                                    />
                                    {showLoader && <Img
                                        src={`${process.env.PUBLIC_URL}/images/spinner.png`} height='20'
                                        className="spinner mb-px ml-2"
                                        alt={`Spinner`}
                                    />}
                                </>
                            }
                        >
                            <Text className="text-white text-left">
                                Login
                            </Text>
                        </Button>
                        {error && error.length > 0 && <p className='errMsg'>{error}</p>}
                    </form>
                </FormProvider>
                <div className="routeContainer mt-4">
                    <NavLink to="/forgot-password">Forgot Password?</NavLink><br />
                    <p className='inline'>Dont have an account? </p>
                    <NavLink to="/register">Sign up here.</NavLink>
                </div>
            </div>
        </div>
    );
}

export default LoginPage;
