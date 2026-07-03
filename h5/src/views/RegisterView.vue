<template>
  <section class="form-screen">
    <div>
      <p class="eyebrow">Player register</p>
      <h1>Create an account after tenant rules load.</h1>
      <p class="muted">Registration is disabled until channel switches and tenant policies are connected.</p>
    </div>

    <form class="panel-form">
      <label>
        Username
        <input placeholder="Choose username" />
      </label>
      <label>
        Referral code
        <input placeholder="Optional" />
      </label>
      <label class="check-row">
        <input type="checkbox" />
        I meet the local eligibility requirements
      </label>
      <button class="btn primary" type="button" disabled>Registration disabled</button>
    </form>
  </section>
</template>
