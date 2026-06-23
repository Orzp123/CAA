"use client";

interface Props {
  tenantCode: string;
}

export default function SsoLoginButton({ tenantCode }: Props) {
  function handleClick() {
    window.location.href = `/api/auth/sso/${encodeURIComponent(tenantCode)}/authorize`;
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-700 bg-gray-800 px-4 py-2.5 text-sm font-medium text-gray-200 transition hover:border-blue-600 hover:bg-blue-950 hover:text-blue-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:ring-offset-gray-900"
      aria-label="使用 SSO 统一认证登录"
    >
      {/* Shield/key icon for SSO */}
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5 text-blue-400"
        aria-hidden="true"
      >
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      </svg>
      SSO 统一认证
    </button>
  );
}
