<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">Player login</p>
      <h1>Sign in before wallet changes.</h1>
      <p class="muted">Demo build only. Real authentication will be wired to the player API later.</p>
    </div>

    <form class="panel-form">
      <label>
        Username
        <input value="member1001" autocomplete="username" />
      </label>
      <label>
        Password
        <input value="demo-password" type="password" autocomplete="current-password" />
      </label>
      <button class="btn primary" type="button" disabled>Login disabled in demo</button>
      <RouterLink class="text-link" to="/register">Create player account</RouterLink>
    </form>
  </section>
</template>
