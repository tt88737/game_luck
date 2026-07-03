<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">GL</span>
        <span>GameLuck</span>
      </RouterLink>
      <nav class="topnav" aria-label="Primary">
        <RouterLink to="/wallet">Wallet</RouterLink>
        <RouterLink to="/games">Games</RouterLink>
        <RouterLink to="/promotions">Rewards</RouterLink>
        <RouterLink to="/redemptions">Redeem</RouterLink>
      </nav>
      <RouterLink class="login-link" to="/login">Login</RouterLink>
    </header>

    <main class="page-wrap">
      <RouterView />
    </main>

    <footer class="tabbar" aria-label="Mobile navigation">
      <RouterLink to="/">Home</RouterLink>
      <RouterLink to="/wallet">Wallet</RouterLink>
      <RouterLink to="/games">Games</RouterLink>
      <RouterLink to="/help">Help</RouterLink>
    </footer>
  </div>
</template>
