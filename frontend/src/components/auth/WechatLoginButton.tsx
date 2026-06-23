"use client";

interface Props {
  tenantCode: string;
}

export default function WechatLoginButton({ tenantCode }: Props) {
  function handleClick() {
    window.location.href = `/api/auth/wechat/authorize?tenantCode=${encodeURIComponent(tenantCode)}`;
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-700 bg-gray-800 px-4 py-2.5 text-sm font-medium text-gray-200 transition hover:border-green-600 hover:bg-green-950 hover:text-green-300 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2 focus:ring-offset-gray-900"
      aria-label="使用微信扫码登录"
    >
      {/* WeChat icon (SVG) */}
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="currentColor"
        className="h-5 w-5 text-green-400"
        aria-hidden="true"
      >
        <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.295.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-3.722-6.348-7.601-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm4.843 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.571 4.546c-4.063.002-7.352 2.921-7.352 6.527 0 3.608 3.289 6.527 7.352 6.527.85 0 1.666-.13 2.424-.35a.74.74 0 0 1 .612.084l1.63.953a.28.28 0 0 0 .143.046.252.252 0 0 0 .252-.252c0-.062-.025-.119-.041-.181l-.334-1.267a.504.504 0 0 1 .183-.569c1.565-1.158 2.569-2.866 2.569-4.991 0-3.606-3.291-6.527-7.438-6.527zm-2.085 3.566c.55 0 .995.453.995 1.01a1.003 1.003 0 0 1-.995 1.012 1.003 1.003 0 0 1-.995-1.012c0-.557.446-1.01.995-1.01zm4.167 0c.55 0 .995.453.995 1.01a1.003 1.003 0 0 1-.995 1.012 1.003 1.003 0 0 1-.994-1.012c0-.557.445-1.01.994-1.01z" />
      </svg>
      微信登录
    </button>
  );
}
